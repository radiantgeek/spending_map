
function routes() { return jsRoutes.controllers.Application }

function showDistricts(map){
    $.ajax({url: "/assets/data/okrug.geojson", dataType: "json", success: function (data) {
        L.geoJson(data, {
            style: function (feature) { return {
                color: "grey", weight: 1, fillColor: "blue", fillOpacity: 0.1
            }; }, onEachFeature: function (feature, layer) {
                layer.bindPopup(feature.properties.name);
            }
        }).addTo(map);
    }});
}
function showReq(bounds, map) {
    if (bounds) {
        // create an orange rectangle
        L.rectangle(bounds, {color: "#ff7800", weight: 1}).addTo(map);
    }
}

function publishResult(id) {
    var r = routes().result_publish();
    var data = {
        id:   $('input[name="search_id"]').val(),
        text: $('input[name="search_text"]').val()
    }
    $.ajax({url: r.url, method: r.method, type: r.dataType, dataType: "json", data: data,
        success: function (data) {
            $('#shareDlg').modal('hide');
        },
        error: function () {
            if (console && console.log) {
                console.log("Error during ajax request. Url="+r.url);
            }
            $('#shareDlg').modal('hide');
        }
    });
}

function showResultMap(search_id, start, bounds) {
    var map = showMap(55.7594, 37.6182, 12)

    var heatCustLayer = L.heatLayer([], {maxZoom: 18}).addTo(map);
    var heatSuppLayer = L.heatLayer([], {maxZoom: 18}).addTo(map);
//        var clusterLayer = L.markerClusterGroup({}).addTo(map);
    var overlayMaps  = { "Customers": heatCustLayer, "Suppliers": heatSuppLayer };
    L.control.layers(null, overlayMaps).addTo(map);
    showReq(bounds, map);

    mapResizer(map);
//    showDistricts(map);

    var hash = new L.Hash(map);

    var pageInited = false;
    var threads = 5;
    var pages = []
    var list = [];
    var downloaded = 0;

    function addPoint(layer, g) {
        if (g.status > 0) {
            layer.addLatLng([g.lat, g.lon]);
        }
    }
    function addRow(e) {
        var сontract = "<a target='_blank' href='"+routes().contract(e.cRegNum).url+"'>"+ e.cRegNum+"</a>";
        var customer = "<a target='_blank' href='"+routes().customer(e.ctRegNum).url+"'>"+ e.ctName+"</a>";
        var supplier = "<a target='_blank' href='"+routes().supplier(e.spInn, e.spKpp).url+"'>"+ e.spName+"</a>, "; //+ s.factualAddress;

        addPoint(heatCustLayer, e.ctAddr);
        addPoint(heatSuppLayer, e.spAddr);
    }

    function pageLoaded(data) {
        var part  = data.data;
        var n     = data.total;
        var p     = data.page;
        var pp    = data.perpage;

        if (!pageInited) {
            pageInited = true;
            for (var i= Math.ceil(n/pp); i > threads; i--) pages.push(i);
        }
        list  = list.concat(part);
        downloaded += data.sended;
        var w = Math.ceil(downloaded*100/n);

        part.forEach(addRow);

        $("#result-search-filtered").html(list.length)
        $("#result-search-current").html(downloaded)
        $("#result-search-max").html(n)
        $("#result-search").width(w+"%");
        $(".progress-percent-label").html(w)

        loadData2D3(data);
    }

    function loadPart(page) {
        if (page < 0) return;
//        if (page > 2) return;

        var r = routes().req(search_id, page);
        $.ajax({url: r.url, type: r.dataType, dataType: "json",
            success: function (data) {
                pageLoaded(data);
                if (pages.length > 0) loadPart(pages.pop());
            },
            error: function () {
                if (console && console.log) {
                    console.log("Error during ajax request. Url="+r.url);
                }
                if (pages.length > 0) loadPart(pages.pop());
            }
        });
    }

    if (start) {
        for (var i = 1; i <= threads; i++) {
            $(function (p) {
                setTimeout(function () {
                    loadPart(p)
                }, (i * 2 - 1) * 1000);
            }(i));
        }
    }
}