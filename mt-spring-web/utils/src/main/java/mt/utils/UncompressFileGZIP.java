package mt.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * -----------------------------------------------------------------------------
 * Used to provide an example of uncompressing a file in the GZIP Format.
 *
 * @author Jeffrey M. Hunter  (jhunter@idevelopment.info)
 * @author http://www.idevelopment.info
 * -----------------------------------------------------------------------------
 * @version 1.0
 */
@Slf4j
public class UncompressFileGZIP {

	public static void doCompressFile(InputStream inputStream, OutputStream out) throws FileNotFoundException, IOException {
		GZIPOutputStream gzipOutputStream = new GZIPOutputStream(out);
		int len;
		byte[] buffer = new byte[1024];
		while((len = inputStream.read(buffer)) != -1){
			gzipOutputStream.write(buffer,0,len);
		}
		inputStream.close();
		gzipOutputStream.close();
	}
	public static void doUncompressFile(InputStream inputStream, OutputStream out) throws FileNotFoundException, IOException {
		log.debug("Opening the compressed file.");
		GZIPInputStream in = new GZIPInputStream(inputStream);
		log.debug("Open the output file.");
		log.debug("Transfering bytes from compressed file to the output file.");
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		log.debug("Closing the file and stream");
		in.close();
		out.close();
	}

	public static void doUncompressFile(String inFileName, String desPath) throws FileNotFoundException, IOException {
		if (!getExtension(inFileName).equalsIgnoreCase("$gzip")) {
			System.err.println("File name must have extension of \".~gzip\"");
			throw new FileNotFoundException("File name must have extension of \".~gzip\"");
		}

		log.debug("Opening the compressed file.");
		GZIPInputStream in = null;
		in = new GZIPInputStream(new FileInputStream(inFileName));
		log.debug("Open the output file.");
		FileOutputStream out = null;
		out = new FileOutputStream(desPath);
		log.debug("Transfering bytes from compressed file to the output file.");
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		log.debug("Closing the file and stream");
		in.close();
		out.close();
	}

	/**
	 * Used to extract and return the extension of a given file.
	 *
	 * @param f Incoming file to get the extension of
	 * @return <code>String</code> representing the extension of the incoming
	 * file.
	 */
	public static String getExtension(String f) {
		String ext = "";
		int i = f.lastIndexOf('.');
		if (i > 0 && i < f.length() - 1) {
			ext = f.substring(i + 1);
		}
		return ext;
	}

	/**
	 * Used to extract the filename without its extension.
	 *
	 * @param f Incoming file to get the filename
	 * @return <code>String</code> representing the filename without its
	 * extension.
	 */
	public static String getFileName(String f) {
		String fname = "";
		int i = f.lastIndexOf('.');

		if (i > 0 && i < f.length() - 1) {
			fname = f.substring(0, i);
		}
		return fname;
	}
}
