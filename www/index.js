$(document).ready(function() {
    $("#get-fallback-contacts").click(function() {
        $.get("/fallback-contacts/get", function (data) {
            $("#fallback-contacts-input").val(data);
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
