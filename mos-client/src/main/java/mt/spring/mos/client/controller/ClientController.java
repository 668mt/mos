package mt.spring.mos.client.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.base.stream.MosEncodeInputStream;
import mt.spring.mos.client.entity.MergeResult;
import mt.spring.mos.client.entity.ResResult;
import mt.spring.mos.client.entity.dto.IsExistsDTO;
import mt.spring.mos.client.entity.dto.MergeFileDto;
import mt.spring.mos.client.entity.dto.Thumb;
import mt.spring.mos.client.service.ClientService;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
	public ResResult upload(MultipartFile file, String pathname, @RequestParam(defaultValue = "false") Boolean cover) throws IOException {
		Assert.notNull(file, "上传的文件不能为空");
		clientService.upload(file.getInputStream(), pathname, file.getSize(), cover);
		return new ResResult("上传成功");
	}
	
	@PostMapping("/mergeFiles")
	@ApiOperation("合并文件")
	public ResResult mergeFiles(@RequestBody MergeFileDto mergeFileDto) throws Exception {
		MergeResult mergeResult = clientService.mergeFiles(mergeFileDto);
		Map<String, Object> params = new HashMap<>();
		params.put("length", mergeResult.getLength());
		if (mergeFileDto.isGetMd5()) {
			try (InputStream inputStream = new MosEncodeInputStream(new FileInputStream(mergeResult.getFile()), mergeFileDto.getDesPathname())) {
				String md5 = DigestUtils.md5Hex(inputStream);
				params.put("md5", md5);
			}
		}
		return new ResResult(params);
	}
	
	@RequestMapping(value = "/deleteFile", method = {RequestMethod.DELETE, RequestMethod.POST})
	@ApiOperation("删除文件")
	public ResResult deleteFile(String pathname) {
		clientService.deleteFile(pathname);
		return new ResResult();
	}
	
	@RequestMapping(value = "/deleteDir", method = {RequestMethod.DELETE, RequestMethod.POST})
	@ApiOperation("删除文件夹")
	public ResResult deleteDir(String path) throws IOException {
		clientService.deleteDir(path);
		return new ResResult();
	}
	
	@RequestMapping(value = "/size", method = {RequestMethod.GET, RequestMethod.POST})
	@ApiOperation("获取文件大小")
	public ResResult size(String pathname) {
		return new ResResult(clientService.getSize(pathname));
	}
	
	@PostMapping("/isExists")
	public ResResult isExists(@RequestBody IsExistsDTO isExistsDTO) {
		Map<String, Boolean> result = clientService.isExists(isExistsDTO);
		return new ResResult(result);
	}
	
	@RequestMapping(value = "/md5", method = {RequestMethod.GET, RequestMethod.POST})
	@ApiOperation("获取md5")
	public ResResult md5(String pathname) {
		return new ResResult(clientService.md5(pathname));
	}
	
	@PutMapping("/moveFile")
	@ApiOperation("移动文件")
	public ResResult moveFile(String srcPathname, String desPathname, @RequestParam(defaultValue = "false") Boolean cover) {
		clientService.moveFile(srcPathname, desPathname, cover);
		return new ResResult("success");
	}
	
	@PostMapping("/thumb")
	@ApiOperation("生成缩略图")
	public ResResult thumb(@RequestParam(defaultValue = "0") Integer seconds,
						   @RequestParam(defaultValue = "400") Integer width,
						   String pathname,
						   String encodeKey) {
		Thumb thumb = clientService.addThumb(pathname, width, seconds, encodeKey);
		return new ResResult(thumb);
	}
	
}
