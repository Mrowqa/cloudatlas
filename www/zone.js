$(document).ready(function() {
    zoneName = document.URL.match(/zone=([a-zA-Z0-9_\/]+)/)[1]; // global variable
    $('#zone-name').val(zoneName);

    function prettifyJSON(str) {
        try {
            return JSON.stringify(JSON.parse(str), null, 2);
        }
        catch (err) {
            return str;
        }
    }

    $("#get-zone-attrs").click(function() {
        $.get("/zones/attributes/get?zone-name=" + zoneName, function (data) {
            $("#zone-attrs").val(prettifyJSON(data));
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

	function getAttributesList(data) {
		var recentZmi = data[data.length - 1].zmi;
		var zone = recentZmi.filter(function (val) { return val.zoneName == zoneName; })[0].zoneAttrs;
		var result = {};
		Reflect.ownKeys(zone).forEach(key => {
			result[key] = zone[key].t;
		});
                return result;
	};

    function getLabelsAndDataFor(data, attrName, attrType) {
        var labels = [];
		var values = [];
		
		for (var i = data.length - 1; i >= 0; i--) {
			var zones = data[i].zmi.filter(function (val) { return val.zoneName == zoneName; });
			if (zones.length == 0) break;
			var zone = zones[0].zoneAttrs;
			if (!zone.hasOwnProperty(attrName)) break;
			var prop = zone[attrName];
			if (prop.t != attrType) break;
			values.unshift(prop.v);
			labels.unshift(data[i].ts);
		}
		
		return {labels:labels, values:values};
    };

	function updateHistoryGraphs() {
		// produce charts:
		$.get("/history", function (data) {
			var canvasHTML = "";
			var navbarHTML = "";
			data = JSON.parse(data);
			var attrsList = getAttributesList(data);
			// create canvases in DOM
			Reflect.ownKeys(attrsList).forEach(key => {
				if (attrsList[key] == "i" || attrsList[key] == "do") {
					var chartName = key + ': ' + attrsList[key];
					var chartId = "chart-" + key;
					canvasHTML += '<h3>' + chartName + '</h3>';
					canvasHTML += '<canvas class="my-4 w-100" id="' + chartId + '" width="900" height="380"></canvas>';

					navbarHTML += `
						<li class="nav-item">
						  <a class="nav-link" href="#` + chartId + `">
							<span data-feather="bar-chart-2"></span>
							` + chartName + `
						  </a>
						</li>`;
				}
				// rest of types is not supported
			});
			$("#graphs-content").html(canvasHTML);
			$("#navbar-charts").html(navbarHTML);
			feather.replace();

			// fill charts with actual data
			Reflect.ownKeys(attrsList).forEach(key => {
				if (attrsList[key] == "i" || attrsList[key] == "do") {
					var graphData = getLabelsAndDataFor(data, key, attrsList[key]);

					var ctx = document.getElementById("chart-" + key);
					var myChart = new Chart(ctx, {
						type: 'line',
						data: {
						  labels: graphData.labels,
						  datasets: [{
							data: graphData.values,
							lineTension: 0,
							backgroundColor: 'transparent',
							borderColor: '#007bff',
							borderWidth: 4,
							pointBackgroundColor: '#007bff'
						  }]
						},
						options: {
                          animation: {
                            duration: 0,
                          },
						  scales: {
							yAxes: [{
							  ticks: {
								beginAtZero: false
							  }
							}]
						  },
						  legend: {
							display: false,
						  }
						}
					});
				}
				// rest of types is not supported
			});
		})
		.fail(function( jqXHR, textStatus, errorThrown ) {
			$("#graphs-content").html("Failed to download zmi history: " + textStatus);
		});
	}

	setInterval(updateHistoryGraphs, 5000); // let it be hardcoded 5s
});
