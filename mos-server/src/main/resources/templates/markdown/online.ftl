<!DOCTYPE html>
<html lang="zh-CN">
<head>
	<title>markdown在线编辑</title>
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

	body, html {
		margin: 0;
		padding: 0;
		width: 100%;
		height: 100%;
	}
</style>

<body>
<div style="margin:0 auto;width: 90%;height:100%;">
	<div style="float: left;width:48%;height:100%;">
		<textarea id="content" style="width: 100%;height:100%;" onkeyup="compile()">
# h1 标题
## h2 标题
### h3 标题
#### h4 标题
##### h5 标题
###### h6 标题


## 水平线

___

---

***


## 文本样式

**This is bold text**

__This is bold text__

*This is italic text*

_This is italic text_

~~Strikethrough~~


## 列表

无序

+ Create a list by starting a line with `+`, `-`, or `*`
+ Sub-lists are made by indenting 2 spaces:
  - Marker character change forces new list start:
    * Ac tristique libero volutpat at
    + Facilisis in pretium nisl aliquet
    - Nulla volutpat aliquam velit
+ Very easy!

有序

1. Lorem ipsum dolor sit amet
2. Consectetur adipiscing elit
3. Integer molestie lorem at massa


1. You can use sequential numbers...
1. ...or keep all the numbers as `1.`

Start numbering with offset:

57. foo
1. bar


## 代码

Inline `code`

Indented code

    // Some comments
    line 1 of code
    line 2 of code
    line 3 of code


Block code "fences"

```
Sample text here...
```

Syntax highlighting

``` js
var foo = function (bar) {
  return bar++;
};

console.log(foo(5));
```
		</textarea>
	</div>
	<div style="float: right;width:48%;height: 100%;margin-left:20px;">
		<div id="result"></div>
	</div>
</div>
<script type="text/javascript">
    function compile() {
        var text = document.getElementById("content").value;
        var converter = new showdown.Converter();
        var html = converter.makeHtml(text);
        document.getElementById("result").innerHTML = html;
    }

    compile();
</script>
</body>
</html>