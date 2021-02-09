<!DOCTYPE html>
<html lang="zh-CN">
<head>
	<title>${title}</title>
	<meta charset="UTF-8">
	<meta name="viewport"
		  content="width=device-width,initial-scale=1,minimum-scale=1,maximum-scale=1,user-scalable=no"/>
	<script src="/js/jquery.min.js" type="text/javascript"></script>
	<link rel="stylesheet" href="/layui/css/layui.css"/>
</head>
<style>
	.img {
		width: 100%;
	}
</style>
<body>
<div class="layui-container">
    <#list imgs as img>
		<div class="layui-row" style="margin-bottom: 20px;">
			<div class="layui-col-md6 layui-col-md-offset3">
				<img class="img" alt="${img.name}" lay-src="${img.url}"/>
			</div>
		</div>
    </#list>
</div>
<div style="height: 50px;">

</div>
</body>
<script src="/layui/layui.js" charset="utf-8"></script>
<script type="text/javascript">
    layui.use('flow', function () {
        var flow = layui.flow;
        //当你执行这样一个方法时，即对页面中的全部带有lay-src的img元素开启了懒加载（当然你也可以指定相关img）
        flow.lazyimg();
    });
</script>
</html>