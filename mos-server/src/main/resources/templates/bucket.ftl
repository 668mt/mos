<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="UTF-8">
	<title>bucket列表</title>
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

	.dir {
		margin-left: 10px;
	}

</style>
<body>
<button class="layui-btn add-bucket-btn">新建Bucket</button>
<table class="layui-table" style="width:950px;">
	<colgroup>
		<col width="350">
		<col width="200">
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
    <#list buckets as bucket>
		<tr>
			<td>
				<svg class="icon svg-icon" aria-hidden="true">
					<use xlink:href="#icon-cunchu"></use>
				</svg>
				<a class="dir"
				   href="${base}/list/${bucket.bucketName}">${bucket.bucketName}</a>
			</td>
			<td></td>
			<td>${bucket.createdDate?string("yyyy-MM-dd HH:mm:ss")}</td>
			<td>
				<button bucketId="${bucket.id}" type="button"
						class="del-bucket-Btn layui-btn layui-btn-danger layui-btn-xs">删除
				</button>
			</td>
		</tr>
    </#list>
	</tbody>
</table>
<script src="${base}/layui/layui.js" type="application/javascript"></script>
<script>
    //一般直接写在一个js文件中
    layui.use(['layer', 'form', 'element', 'jquery'], function () {
        var layer = layui.layer
            , form = layui.form
            , element = layui.element
            , $ = layui.jquery;
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

        $(".add-bucket-btn").click(function () {
            layer.prompt({
                formType: 2,
                value: '',
                title: '请输入BucketName'
            }, function (bucketName, index, elem) {
                $.ajax({
                    url: '${base}/member/bucket',
                    type: 'post',
                    data: {
                        bucketName: bucketName
                    },
                    success: function (result) {
                        if (result.status === 'ok') {
                            success("新增成功", function () {
                                location.reload();
                            });
                        } else {
                            error("新增失败:" + result.message);
                        }
                    }
                });
                layer.close(index);
            });
        });

        $(".del-bucket-Btn").click(function () {
            var bucketId = $(this).attr("bucketId");
            layer.confirm("是否确认删除？", function (index) {
                $.ajax({
                    url: '${base}/member/bucket',
                    type: 'delete',
                    data: {
                        id: bucketId
                    },
                    success: function (result) {
                        if (result.status === 'ok') {
                            success('删除成功', function () {
                                location.reload();
                            });
                        } else {
                            error(layer.message);
                        }
                    }
                });
                layer.close(index);
            });
        });
    });
</script>
</body>
</html>