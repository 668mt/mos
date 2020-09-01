<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="UTF-8">
	<title>文件列表</title>
	<link rel="stylesheet" href="${base}/layui/css/layui.css"/>
	<script src="${base}/iconfont/iconfont.js"></script>
</head>
<style>
	.icon {
		width: 1.25em;
		height: 1.25em;
		vertical-align: -0.15em;
		fill: currentColor;
		overflow: hidden;
	}

	.parentDir {
		margin: 5px;
		color: #000000;
		font-size: 14px;
	}

	.lastDir {
		font-size: 14px;
	}

	.parentDir:hover {
		color: #5FB878;
	}

	.dir {
		margin-left: 10px;
	}

	.file {
		margin-left: 10px;
	}
</style>
<body>
<button class="layui-btn upload-btn">上传文件</button>
<table class="layui-table" style="width:950px;">
	<colgroup>
		<col width="350">
		<col width="200">
		<col>
		<col>
	</colgroup>
	<thead>
	<tr>
		<th>文件名</th>
		<th>文件大小</th>
		<th>创建时间</th>
		<th>操作</th>
	</tr>
	</thead>
	<tbody>
    <#if parentDirs?size gt 0>
		<tr>
			<td>
                <#if lastDir??>
					<a class="lastDir" href="${base}/list/${bucketName}${lastDir.path}">
						<svg class="icon svg-icon" aria-hidden="true">
							<use xlink:href="#icon-fanhui"></use>
						</svg>
					</a>
                </#if>
                <#list parentDirs as dir>
					<a class="parentDir" href="${base}/list/${bucketName}${dir.path}"> ${dir.getName()} </a>
                </#list>
				<span style="margin:5px;color: #999999;">${currentDir.getName()}</span>
			</td>
			<td></td>
			<td></td>
			<td></td>
		</tr>
    <#else>
		<tr>
			<td>
				<a href="${base}/list">
					<svg class="icon svg-icon" aria-hidden="true">
						<use xlink:href="#icon-fanhui"></use>
					</svg>
				</a>
			</td>
			<td></td>
			<td></td>
			<td></td>
		</tr>
    </#if>

    <#list dirs as dir>
		<tr>
			<td>
				<svg class="icon svg-icon" aria-hidden="true">
					<use xlink:href="#icon-weibiaoti-_huabanfuben"></use>
				</svg>
				<a class="dir"
				   href="${base}/list/${bucketName}${dir.path}">${dir.getName()}</a>
			</td>
			<td></td>
			<td>${dir.createdDate?string("yyyy-MM-dd HH:mm:ss")}</td>
			<td>
				<button path="${dir.path}" type="button"
						class="deleteDirBtn layui-btn layui-btn-danger layui-btn-xs">删除
				</button>
			</td>
		</tr>
    </#list>
    <#list files as file>
		<tr>
			<td>
				<svg class="icon svg-icon" aria-hidden="true">
					<use xlink:href="#icon-file"></use>
				</svg>
				<a class="file" target="_blank"
				   href="${base}/oss/${bucketName}${file.pathname}">${file.getFileName()}</a>
			</td>
			<td>${file.getReadableSize()}</td>
			<td>${file.createdDate?string("yyyy-MM-dd HH:mm:ss")}</td>
			<td>
				<button pathname="${file.pathname}" type="button"
						class="deleteFileBtn layui-btn layui-btn-danger layui-btn-xs">删除
				</button>
			</td>
		</tr>
    </#list>
	</tbody>
</table>
</body>
<div id="form" style="width: 600px;display: none;">
	<div class="layui-progress" lay-filter="uploadProgress" style="width: 100%;">
		<div class="layui-progress-bar" lay-percent="0%"></div>
	</div>
	<div class="layui-form" style="width:400px;margin:10px auto;" lay-filter="uploadForm">
		<div class="layui-form-item">
			<label class="layui-form-label">bucket</label>
			<div class="layui-input-block">
				<select id="bucket" name="bucket" lay-filter="bucket">
                    <#list buckets as bucket>
						<option <#if bucket.bucketName == bucketName>selected</#if>
								value="${bucket.bucketName}">${bucket.bucketName}</option>
                    </#list>
				</select>
			</div>
		</div>
		<div class="layui-form-item">
			<label class="layui-form-label">pathname</label>
			<div class="layui-input-block">
				<input id="pathname" type="text" name="pathname" <#if currentDir??>value="${currentDir.path}"</#if>
					   placeholder="请输入pathname"
					   autocomplete="off"
					   class="layui-input">
			</div>
		</div>
		<div class="layui-form-item">
			<div class="layui-input-block">
				<button id="uploadChoose" class="layui-btn layui-btn-normal">选择文件</button>
			</div>
		</div>
		<div class="layui-form-item">
			<div class="layui-input-block">
				<button id="uploadSubmit" class="layui-btn layui-btn-fluid">上传</button>
			</div>
		</div>
	</div>
</div>
<script src="${base}/layui/layui.js" type="application/javascript"></script>
<script type="application/javascript">
    //一般直接写在一个js文件中
    layui.use(['layer', 'form', 'element', 'jquery', 'upload'], function () {
        var layer = layui.layer
            , form = layui.form
            , element = layui.element
            , upload = layui.upload
            , $ = layui.jquery;
        var uploadLayerIndex;
        var currentPath = '';
        <#if currentDir??>
        currentPath = '${currentDir.path}';
        </#if>
        var initForm = function () {
            form.val('uploadForm', {
                bucket: '${bucketName}',
                pathname: currentPath
            });
        }

        var success = function (msg, func) {
            layer.msg(msg, {
                icon: 1,
                time: 2000,
                offset: 't',
                anim: 1
            }, func);
        };
        var error = function (msg, func) {
            layer.msg(msg, {
                icon: 2,
                time: 2000,
                offset: 't',
                anim: 1
            }, func);
        };

        $(".upload-btn").click(function () {
            initForm();
            uploadLayerIndex = layer.open({
                type: 1,
                title: '文件上传',
                content: $('#form'),
                area: ['600px', '300px']
            });
        });

        //执行实例
        var uploadInverval;
        var beforeUploadIngress = 0;
        var afterUploadIngress = 0;
        var updateIngressFunc = function (beforeUploadIngress, afterUploadIngress) {
            afterUploadIngress = afterUploadIngress ? afterUploadIngress : 0;
            var n = beforeUploadIngress * 0.8 + afterUploadIngress * 0.2;
            var percent = n + '%' //获取进度百分比
            element.progress('uploadProgress', percent);
        }

        var uploadInst = upload.render({
            elem: '#uploadChoose' //绑定元素
            , url: '${base}/upload/' //上传接口
            , auto: false
            , accept: 'file'
            , bindAction: '#uploadSubmit'
            , multiple: false
            , progress: function (n, elem) {
                beforeUploadIngress = n;
                updateIngressFunc(beforeUploadIngress, afterUploadIngress);
            }
            , choose: function (obj) {
                beforeUploadIngress = 0;
                afterUploadIngress = 0;
                var fileName;
                var files = obj.pushFile();
                for (name in files) {
                    fileName = files[name].name;
                }
                for (name in files) {
                    if (fileName != files[name].name) {
                        delete files[name];
                    }
                }

                var $pathname = $("#pathname");
                if (currentPath === '/') {
                    $pathname.val("/" + fileName);
                } else {
                    $pathname.val(currentPath + "/" + fileName);
                }
                $("#chooseFile").remove();
                $("#uploadChoose").after('<span id="chooseFile" style="color:#999;margin-left:10px;">' + fileName + '</span>');
            }
            , before: function (obj) {
                this.url = '${base}/upload/' + $("#bucket").val();
                this.data = {
                    pathname: $("#pathname").val()
                };
                $.get("${base}/upload/ingress/reset", {
                    bucketName: '${bucketName}',
                    pathname: $("#pathname").val()
                });
                uploadInverval = setInterval(function () {
                    $.get("${base}/upload/ingress", {
                        bucketName: '${bucketName}',
                        pathname: $("#pathname").val()
                    }, function (result) {
                        afterUploadIngress = result.result ? result.result * 100 : 0;
                        updateIngressFunc(beforeUploadIngress, afterUploadIngress);
                    });
                }, 500);
            }
            , done: function (res) {
                //上传完毕回调
                if (res.status === 'ok') {
                    success("上传成功!");
                    layer.close(uploadLayerIndex);
                } else {
                    error("上传失败：" + res.message);
                }
                clearInterval(uploadInverval);
                updateIngressFunc(100, 100);
            }
            , error: function () {
                //请求异常回调
                error("请求超时，请检查网络情况");
                clearInterval(uploadInverval);
            }
        });

        //删除文件按钮
        $(".deleteFileBtn").click(function () {
            var pathname = $(this).attr("pathname");
            layer.confirm('是否删除 ' + pathname, function (index) {
                $.ajax({
                    url: "${base}/member/resource/${bucketName}/delFile",
                    type: "delete",
                    data: {
                        pathname: pathname
                    },
                    success: function (result) {
                        if (result.status === 'ok') {
                            success('删除成功', function () {
                                location.reload();
                            });
                        } else {
                            error('删除失败:' + result.message);
                        }
                    }
                })
                layer.close(index);
            });
        });

        //删除文件夹按钮
        $(".deleteDirBtn").click(function () {
            var path = $(this).attr("path");
            layer.confirm('是否删除 ' + path + '？文件夹下的所有文件都会被删除！', function (index) {
                $.ajax({
                    url: "${base}/member/resource/${bucketName}/delDir",
                    type: "delete",
                    data: {
                        path: path
                    },
                    success: function (result) {
                        if (result.status === 'ok') {
                            success('删除成功', function () {
                                location.reload();
                            });
                        } else {
                            error('删除失败:' + result.message);
                        }
                    }
                })
                layer.close(index);
            });
        });
    });

</script>
</html>