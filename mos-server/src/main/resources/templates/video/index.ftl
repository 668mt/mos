<!DOCTYPE html>
<html lang="zh-CN">
<head>
	<title>${title}</title>
	<meta charset="UTF-8">
	<meta name="viewport"
		  content="width=device-width,initial-scale=1,minimum-scale=1,maximum-scale=1,user-scalable=no"/>
	<link rel="stylesheet" href="/css/bootstrap.min.css"/>
	<script src="/ckplayer/ckplayer.js" type="text/javascript"></script>
	<script src="/js/jquery.min.js" type="text/javascript"></script>
</head>
<style>
	.player {
		display: block;
		width: 100%;
		height: 350px;
	}
	body,html{
		background: #0C0C0C;
	}
</style>
<body onresize="resize()">
<div class="col-xs-12 col-sm-10 col-sm-offset-1 col-md-8 col-md-offset-2">
	<div id="videoContainer" style="width: 100%; height: 100%;" class="player">
		<video></video>
	</div>
</div>
</body>
<script type="text/javascript">
    let player;
    $(function () {
        resize();
        let url = '${url}';
        playVideo(url);
    })

    function resize() {
        const $videoContainer = $("#videoContainer");
        let width = $videoContainer.width();
        let height = width / 1.6;
        let marginTop = ($(document).height() - height) / 2;
        $videoContainer.css("height", height + 'px');
        $videoContainer.css("margin-top", marginTop + 'px');
    }

    function play(url, html5m3u8) {
        let volume = getCookie('player-volume');
        volume = volume ? volume : 0.5;
        let videoObject = {
            container: '#videoContainer',//“#”代表容器的ID，“.”或“”代表容器的class
            variable: 'player',//该属性必需设置，值等于下面的new chplayer()的对象
            video: url,
            click: true,
            html5m3u8: html5m3u8,
            autoplay: false,
            loaded: 'loadedHandler',
            volume: volume
        };
        player = new ckplayer(videoObject);
    }

    //直接视频地址播放
    function playMp4(url) {
        play(url, false);
    }

    function playM3U8(url) {
        play(url, true);
    }

    function playVideo(url) {
        if (url.indexOf('.m3u8') !== -1) {
            playM3U8(url);
        } else {
            playMp4(url);
        }
    }

    function loadedHandler() {
        player.videoPlay();
        player.addListener('volume', volumeChangeHandler);
    }

    let getCookie = function (name) {
        var cookieValue = "";
        var search = escape(name) + "=";
        if (document.cookie.length > 0) {
            offset = document.cookie.indexOf(search);
            if (offset !== -1) {
                offset += search.length;
                end = document.cookie.indexOf(";", offset);
                if (end === -1)
                    end = document.cookie.length;
                cookieValue = decodeURIComponent((document.cookie.substring(offset, end)))
            }
        }
        return cookieValue;
    }
    let setCookie = function (name, value, hours, path, domain, secure) {
        var expire = "";
        var pathstr = "";
        var domainstr = "";
        var securestr = "";
        if (hours != null) {
            expire = new Date((new Date()).getTime() + hours * 3600000);
            expire = "; expires=" + expire.toGMTString();
        }
        if (path != null) {
            pathstr = "; path=" + path;
        }
        if (domain != null) {
            domainstr = "; domain=" + domain;
        }
        if (secure != null) {
            securestr = "; secure"
        }

        document.cookie = name + "=" + escape(value) + expire + pathstr + domainstr + securestr;
    }

    function volumeChangeHandler(vol) {
        let volume = parseFloat(vol);
        if (isNaN(volume)) {
            volume = 0;
        }
        setCookie('player-volume', volume, 24 * 30);
    }

</script>
</html>