import SparkMD5 from 'spark-md5';

/**
 * 分片大小：固定 100MB。
 *
 * 最后一个分片为余数（可能小于 100MB）；
 * 文件本身小于 100MB 时只有 1 个分片。
 */
export const PART_SIZE = 100 * 1024 * 1024;

export interface SplitResult {
  totalSize: number;
  chunks: number;
  partSize: number;
  uploadParts: Blob[];
}

/**
 * 将文件切分为分片。
 *
 * partSize 固定为 PART_SIZE（100MB），不足一个分片的余数单独作为最后一个分片。
 */
export function splitFile(file: File, partSize: number = PART_SIZE): SplitResult {
  const totalSize = file.size;
  // 空文件或小于一个分片时，按 1 个分片处理
  const chunkSize = totalSize > 0 ? Math.min(partSize, totalSize) : partSize;
  const chunks = Math.max(1, Math.ceil(totalSize / chunkSize));

  const uploadParts: Blob[] = [];
  for (let i = 0; i < chunks; i++) {
    if (i === chunks - 1) {
      uploadParts.push(file.slice(i * chunkSize, totalSize));
    } else {
      uploadParts.push(file.slice(i * chunkSize, (i + 1) * chunkSize));
    }
  }
  return { totalSize, chunks, partSize: chunkSize, uploadParts };
}

/**
 * 计算 Blob 的 MD5（分 2MB 读取，避免一次性占用大内存）。
 *
 * @param onProgress 每个分片读取完成时回调 (totalChunks, currentChunk)
 */
export function md5File(
  file: Blob,
  onProgress?: (total: number, current: number) => void,
): Promise<string> {
  return new Promise((resolve, reject) => {
    const chunkSize = 2 * 1024 * 1024;
    const chunks = Math.max(1, Math.ceil(file.size / chunkSize));
    let currentChunk = 0;
    const spark = new SparkMD5.ArrayBuffer();
    const fileReader = new FileReader();

    fileReader.onload = (e) => {
      spark.append(e.target!.result as ArrayBuffer);
      currentChunk += 1;
      onProgress?.(chunks, currentChunk);
      if (currentChunk < chunks) {
        loadNext();
      } else {
        resolve(spark.end());
      }
    };

    fileReader.onerror = () => {
      reject(new Error('文件读取失败，无法计算 MD5'));
    };

    function loadNext() {
      const start = currentChunk * chunkSize;
      const end = start + chunkSize >= file.size ? file.size : start + chunkSize;
      fileReader.readAsArrayBuffer(file.slice(start, end));
    }

    loadNext();
  });
}
