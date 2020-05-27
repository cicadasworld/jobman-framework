//
// 2018/9/6 16:29:16
//
function dumper_add_section(div, sectionText)
{
    var h = document.createElement("h3");
    h.innerHTML = "<span style='background: #EEEECC;'>" + sectionText + "</span>";
    div.appendChild(h);
}

function dumper_add_separator(div)
{
    var h = document.createElement("div");
    h.style.left = '20px';
    h.style.height = '10px';
    div.appendChild(h);
}

function dumper_create_links(gtdumpGlobal, parentDIV, linkList)
{
    var appBaseURL = gtdumpGlobal.appBaseURL;

    for (var i = 0; i < linkList.length; ++ i) {
        var str = linkList[i];
        if (str == null) {
            continue;
        }

        if (str == 'separator') {
            dumper_add_separator(parentDIV);
            continue;
        }
        else {
            if (str.indexOf('{') >= 0) {
                str = str.replace('{appBaseURL}', appBaseURL);
            }

            var div = document.createElement("div");
            //div.className = '';
            div.style.left = '20px';
            div.innerHTML = str;
            parentDIV.appendChild(div);
        }
    }
}

function dumper_lpad2(n)
{
    if (n >= 10) {
        return '' + n;
    } else {
        return '0' + n;
    }
}
function dumper_lpad3(n)
{
    if (n >= 100) {
        return '' + n;
    } else if (n >= 10) {
        return '0' + n;
    } else {
        return '00' + n;
    }
}

function dumper_format_date(d)
{
    let year = d.getFullYear();
    let month = d.getMonth() + 1;
    let day = d.getDate();
    let hour = d.getHours();
    let minute = d.getMinutes();
    let sec = d.getSeconds();
    let s = year + '/' + dumper_lpad2(month) + '/' + dumper_lpad2(day) + ' ';
    s += (dumper_lpad2(hour) + ':' + dumper_lpad2(minute) + ':' + dumper_lpad2(sec));
    return s;
}

// 将毫秒格式化成对人友好的标识
function dumper_format_timespan(ms)
{
    let sec = ms/1000.0;
    let hour = parseInt(sec/3600);
    sec -= (hour * 3600);

    let minute = parseInt(sec/60);
    sec -= (minute * 60);

    let nsec = parseInt(sec); //秒的整数部分
    let fsec = (sec - nsec).toFixed(3); //秒的小数部分

    return dumper_lpad3(hour)
        + ':' + dumper_lpad2(minute)
        + ':' + dumper_lpad2(nsec)
        + ('' + fsec).substring(1);
}

