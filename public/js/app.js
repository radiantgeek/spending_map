
function showMap(lat, lon, zoom) {
    var mapAttribution = 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://opendatacommons.org/licenses/odbl/">ODbL</a> license. ';

    var center = [lat, lon, zoom];
    var map = L.map('map').setView(center, zoom);
    L.tileLayer('http://tile.openstreetmap.org/{z}/{x}/{y}.png', { minZoom: 7, maxZoom: 18, attribution: mapAttribution }).addTo(map);
    return map;
}

function mapResizer(map) {
    var map_margin = 130;

    function resize(){
        $('#map').css("height", ($(window).height() - map_margin));
        map.invalidateSize();
    }

    $(window).on("resize", resize);
    resize();
}

$(function() {

    $("#loading").hide();
    $( document ).ajaxStart(function() { $( "#loading" ).show(); });
    $( document ).ajaxStop(function() {  $( "#loading" ).hide(); });

});