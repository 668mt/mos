<!DOCTYPE html>
<html lang="zh-CN">
<head>
	<title>${title}</title>
	<script type="text/javascript" src="/js/showdown.min.js"></script>
	<script src="/js/jquery.min.js" type="text/javascript"></script>
</head>
<style>
	body {
		font-family: "Helvetica Neue", Helvetica, Microsoft Yahei, Hiragino Sans GB, WenQuanYi Micro Hei, sans-serif;
		font-size: 16px;
		line-height: 1.42857143;
		color: #333;
		background-color: #fff;
	}

	ul li {
		line-height: 24px;
	}

	blockquote {
		border-left: #eee solid 5px;
		padding-left: 20px;
	}

	code {
		color: #D34B62;
		background: #F9F2F4;
	}
</style>
<body>
<div>
	<div id="result"></div>
</div>
<script type="text/javascript">

    $(function () {
        $.ajax({
            url: '${url}',
            method: 'get',
            success: function (data) {
                var converter = new showdown.Converter();
                $("#result").html(converter.makeHtml(data));
            }
        });
    })

</script>
</body>
</html>