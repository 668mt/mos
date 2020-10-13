<!DOCTYPE html>
<html lang="zh-CN">
<head>
	<title>${title}</title>
	<script type="text/javascript" src="/js/showdown.min.js"></script>

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
	<textarea id="content" style="height:400px;width:600px;display:none;" onkeyup="compile()">${content}</textarea>
	<div id="result"></div>

</div>
<script type="text/javascript">
    function compile() {
        var text = document.getElementById("content").value;
        var converter = new showdown.Converter();
        var html = converter.makeHtml(text);
        document.getElementById("result").innerHTML = html;
    }

    window.onload = function () {
        compile();
    }
</script>
</body>
</html>