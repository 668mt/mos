package mt.spring.mos.client.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.client.entity.ResResult;
import mt.spring.mos.client.entity.dto.MergeFileDto;
import mt.spring.mos.client.service.ClientService;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author Martin
 * @Date 2020/5/15
 */
@RestController
@RequestMapping("/client")
@Api(tags = "文件上传接口")
@Slf4j
public class ClientController {
	@Autowired
	private ClientService clientService;
	
	@PostMapping("/upload")
	@ApiOperation("上传文件")
	public ResResult upload(MultipartFile file, String pathname) throws IOException {
		Assert.notNull(file, "上传的文件不能为空");
		clientService.upload(file.getInputStream(), pathname, file.getSize());
		return new ResResult("上传成功");
	}
	
	@PostMapping("/mergeFiles")
	@ApiOperation("合并文件")
	public ResResult mergeFiles(@RequestBody MergeFileDto mergeFileDto) throws IOException {
		File file = clientService.mergeFiles(mergeFileDto);
		Map<String, Object> params = new HashMap<>();
		params.put("length", file.length());
		if (mergeFileDto.isGetMd5()) {
			try (InputStream inputStream = new FileInputStream(file)) {
				String md5 = DigestUtils.md5Hex(inputStream);
				params.put("md5", md5);
			}
		}
		return new ResResult(params);
	}
	
	@DeleteMapping("/deleteFile")
	@ApiOperation("删除文件")
	public ResResult deleteFile(String pathname) {
		clientService.deleteFile(pathname);
		return new ResResult();
	}
	
	@DeleteMapping("/deleteDir")
	@ApiOperation("删除文件夹")
	public ResResult deleteDir(String path) throws IOException {
		clientService.deleteDir(path);
		return new ResResult();
	}
	
	@GetMapping("/size")
	@ApiOperation("获取文件大小")
	public ResResult size(String pathname) {
		return new ResResult(clientService.getSize(pathname));
	}
	
	@PutMapping("/moveFile")
	@ApiOperation("移动文件")
	public ResResult moveFile(String srcPathname, String desPathname) {
		clientService.moveFile(srcPathname, desPathname);
		return new ResResult("success");
	}
	
}
