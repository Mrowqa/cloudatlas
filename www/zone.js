$(document).ready(function() {
    zoneName = document.URL.match(/zone=([a-zA-Z0-9_\/]+)/)[1];
    $('#zone-name').val(zoneName);

    $("#get-zone-attrs").click(function() {
        $.get("/zones/attributes/get?zone-name=" + zoneName, function (data) {
            $("#zone-attrs").val(data);
            alert("OK");
        })
        .fail(function( jqXHR, textStatus, errorThrown ) {
            alert(textStatus);
        });
    });

    $("#set-zone-attrs").click(function() {
        var zoneData = {
            "zone-name": zoneName,
            "zone-attrs": $("#zone-attrs").val()
        };
        $.post("/zones/attributes/set", zoneData, function (data) {
            alert(data);
            console.log(data);
        })
        .fail(function( jqXHR, textStatus, errorThrown ) {
            alert(textStatus);
        });
    });

    // produce charts:
    /*$.get("/zones/get", function (data) {
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
    });*/
});
