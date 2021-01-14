<!DOCTYPE html>
<html lang="zh-CN">
<head>
	<title>${title}</title>
	<meta charset="UTF-8">
	<meta name="viewport"
		  content="width=device-width,initial-scale=1,minimum-scale=1,maximum-scale=1,user-scalable=no"/>
	<script src="/js/jquery.min.js" type="text/javascript"></script>
</head>
<style>
	body {
		background-color: #faeed7;
		font-size: 16px;
		line-height: 22px;
	}
</style>
<body>
<div id="content">
</div>
</body>
<script type="text/javascript">
	$(function () {
		$.ajax({
			url:'${url}',
			method:'get',
			success:function(data){
			    data = data.replace(/\n/g,'<br/>');
			    $("#content").html(data);
			}
		})
    })
</script>
</html>