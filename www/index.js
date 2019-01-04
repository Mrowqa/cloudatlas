$(document).ready(function() {
    function prettifyJSON(str) {
        try {
            return JSON.stringify(JSON.parse(str), null, 2);
        }
        catch (err) {
            return str;
        }
    }

    $("#install-query-button").click(function() {
        var queryData = {
            "query-name": $("#query-name").val(),
            "query-value": $("#query-value").val()
        };
        $.post("/query/install", queryData, function (data) {
            alert(data);
            console.log(data);
        })
        .fail(function( jqXHR, textStatus, errorThrown ) {
            alert(textStatus);
        });
    });

    $("#uninstall-query-button").click(function() {
        var queryData = {
            "query-name": $("#query-name-uninstall").val()
        };
        $.post("/query/uninstall", queryData, function (data) {
            alert(data);
            console.log(data);
        })
        .fail(function( jqXHR, textStatus, errorThrown ) {
            alert(textStatus);
        });
    });

    $("#get-all-queries").click(function() {
        $.get("/query/get", function (data) {
            $("#all-queries-input").val(prettifyJSON(data));
            alert("OK");
        })
        .fail(function( jqXHR, textStatus, errorThrown ) {
            alert(textStatus);
        });
    });

    $("#get-fallback-contacts").click(function() {
        $.get("/fallback-contacts/get", function (data) {
            $("#fallback-contacts-input").val(prettifyJSON(data));
            alert("OK");
        })
        .fail(function( jqXHR, textStatus, errorThrown ) {
            alert(textStatus);
        });
    });

    $("#set-fallback-contacts").click(function() {
        var contacts = {contacts: $("#fallback-contacts-input").val()};
        $.post("/fallback-contacts/set", contacts, function (data) {
            alert(data);
            console.log(data);
        })
        .fail(function( jqXHR, textStatus, errorThrown ) {
            alert(textStatus);
        });
    });

    // get zone list:
    $.get("/zones/get", function (data) {
        var listHTML = "";
        data = JSON.parse(data);
        for (var ind in data.v) {
            var zone = data.v[ind].v;
            listHTML += '<a href="/static/zone.html?zone=' + zone + '" class="list-group-item list-group-item-action">' + zone + "</a>";
        }
        $("#zone-list-content").html(listHTML);
    })
    .fail(function( jqXHR, textStatus, errorThrown ) {
        $("#zone-list-content").html("Failed to download zone list: " + textStatus);
    });
});
