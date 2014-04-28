// -------------------------------------------------------------------------------------------------------------------
// ### Create Chart Objects
//var fluctuationChart    = dc.barChart("#fluctuation-chart");
var dateChart           = dc.barChart("#date-chart");
var priceChart          = dc.barChart("#price-chart");
var customerBubbleChart = dc.bubbleChart("#customerBubble-chart");
var supplierBubbleChart = dc.bubbleChart("#supplierBubble-chart");
var quarterSumChart     = dc.pieChart("#quarterSum-chart");
var quarterCountChart   = dc.pieChart("#quarterCount-chart");
var dayOfWeekSumChart   = dc.rowChart("#dayOfWeekSum-chart");
var dayOfWeekCountChart = dc.rowChart("#dayOfWeekCount-chart");

// -------------------------------------------------------------------------------------------------------------------
var maxWidth        = $(".tab-content").width()-50;

//### Create Crossfilter Dimensions and Groups
var ndx             = crossfilter([]);
var renderer        = false;
var numberFormat    = d3.format(",.0f");
var ctDimension     = ndx.dimension(function (d) { return d.ctName; });
var spDimension     = ndx.dimension(function (d) { return d.spName; });
var weekDayNames    = ["", "Пн","Вт","Ср","Чт","Пт","Сб", "Вс"];

// -------------------------------------------------------------------------------------------------------------------

function loadData2D3(data){
    var isoFormat  = d3.time.format.iso;
    var dateFormat = d3.time.format("%d.%m.%Y");

    data.data.forEach(function (d) {
        d.sd        = isoFormat.parse(d.cSignDate);
        d.date      = dateFormat(d.sd);
        d.month     = d3.time.month(d.sd);
        d.ed        = isoFormat.parse(d.cExecDate);
        d.volume    = +d.cPrice;
    });
    ndx.add(data.data);
    scheduledRender();
}
function scheduledRender() {
    if (!renderer) {
        renderer = setTimeout(function(){ dc.redrawAll(); renderer = false; }, 1000);
    }
}

// -------------------------------------------------------------------------------------------------------------------

function dateChartInit(startDate, stopDate) {
    var dateDimension   = ndx.dimension(function (d) { return d.sd; });
    var dateGroup       = dateDimension.group();

    dateChart.width(maxWidth).height(100)
        .margins({top: 0, right: 50, bottom: 20, left: 40})
        .dimension(dateDimension).group(dateGroup)
        .elasticY(true).centerBar(true).gap(2)
        .x(d3.time.scale().domain([startDate, stopDate]))
        .renderHorizontalGridLines(false)
//        .alwaysUseRounding(true)
        .xUnits(d3.time.days);
}

function priceChartInit(startDate, stopDate) {
    var priceDimension  = ndx.dimension(function (d) { return d.volume; });
    var priceGroup      = priceDimension.group();

    priceChart.width(maxWidth).height(100)
        .margins({top: 0, right: 50, bottom: 20, left: 40})
        .dimension(priceDimension).group(priceGroup)
//        .elasticX(true)
        .elasticY(true).centerBar(true).gap(2)
        .x(d3.scale.linear().domain([0, 30*1000*1000]))
//        .renderHorizontalGridLines(true)
//        .alwaysUseRounding(true)
//        .xUnits(d3.time.days);
}

function quarterChartInit(){
    var quarter = ndx.dimension(function (d) {
        var month = d.sd.getMonth();
        if (month <= 2)                     return "I";
        else if (month > 2 && month <= 5)   return "II";
        else if (month > 5 && month <= 8)   return "III";
        else                                return "IV";
    });
    var quarterSumGroup   = quarter.group().reduceSum(function (d) { return d.volume; });
    var quarterCountGroup = quarter.group();

    quarterSumChart.width(maxWidth/4).height(180).radius(80).innerRadius(30)
        .dimension(quarter).group(quarterSumGroup)
        .label(function (d) { return d.key + " кв"; })
        .title(function (d) { return d.key + " квартал: контрактов на сумму " + numberFormat(d.value) + " руб"; });
    quarterCountChart.width(maxWidth/4).height(180).radius(80).innerRadius(30)
        .dimension(quarter).group(quarterCountGroup)
        .label(function (d) { return d.key + " кв"; })
        .title(function (d) { return d.key + " квартал: " + numberFormat(d.value) + " контрактов"; });
}

function dayOfWeekChartInit(){
    var dayOfWeek = ndx.dimension(function (d) {
        var day = d.sd.getDay();
        if (day == 0) day = 7;
        return day+"."+weekDayNames[day];
    });
    var dayOfWeekGroup = dayOfWeek.group();
    var dayOfWeekSumGroup = dayOfWeek.group().reduceSum(function (d) { return d.volume; });

    dayOfWeekSumChart.width(maxWidth/4).height(220)
        .margins({top: 20, left: 10, right: 10, bottom: 20})
        .group(dayOfWeekSumGroup).dimension(dayOfWeek)
        .ordinalColors(['#6baed6', '#9ecae1', '#c6dbef', '#dadaeb', '#dadaeb', '#3182bd', '#3182bd'])
        .label(function (d) { return d.key.split(".")[1]; })
        .title(function (d) { return "контрактов на сумму "+numberFormat(d.value)+" руб"; })
//        .x(d3.scale.linear().domain(names))
        .elasticX(true);

    dayOfWeekCountChart.width(maxWidth/4).height(220)
        .margins({top: 20, left: 10, right: 10, bottom: 20})
        .group(dayOfWeekGroup).dimension(dayOfWeek)
        // assign colors to each value in the x scale domain
        .ordinalColors(['#6baed6', '#9ecae1', '#c6dbef', '#dadaeb', '#dadaeb', '#3182bd', '#3182bd'])
        .label(function (d) { return d.key.split(".")[1]; })
        .title(function (d) { return numberFormat(d.value)+" контрактов"; })
        .elasticX(true)
        .xAxis().ticks(4);
}

function dataTableInit() {
    var dateDimension = ndx.dimension(function (d) { return d.sd; });

    function _link(t) { return '&nbsp;<a href="http://clearspending.ru/'+t+'" target="_blank"><i class="fa fa-external-link"></i></a>' }

    dc.dataTable(".dc-data-table").dimension(dateDimension)
        .group(function (d) {
            var format = d3.format("02d");
            return d.sd.getFullYear() + "/" + format((d.sd.getMonth() + 1));
        })
        .columns([
            function (d) { return d.cRegNum + _link("contract/"+ d.cRegNum);  },
            function (d) { return d.date; },
            function (d) { return d.ctName+_link("customer/"+d.ctRegNum); },
            function (d) { return (d.spName)? (d.spName+_link("supplier/inn="+d.spInn+"&kpp="+d.spKpp)) : "" },
            function (d) { return numberFormat(d.volume); }
        ])
        .sortBy(function (d) { return d.volume; })
        .order(d3.descending)
        .renderlet(function (table) {
            table.selectAll(".dc-table-group").classed("info", true);
        });
}

function customerBubbleChartInit() {
    var ctSumGroup      = ctDimension.group().reduce(
        function (p, v) { p.volume += +v.volume; p.counts += 1; return p; },
        function (p, v) { p.volume -= +v.volume; p.counts -= 1; return p; },
        function ()     { return {volume: 0, counts: 0} }
    );

    customerBubbleChart.width(maxWidth/2).height(250)
        .margins({top: 10, right: 50, bottom: 30, left: 40})
        .dimension(ctDimension).group(ctSumGroup)
        .data(function(group) { return group.top(100); })
        .colors(colorbrewer.RdYlGn[9].reverse()).colorDomain([0, 30])
        .colorAccessor(function (d) {       return d.value.counts; })
        .keyAccessor(function (p) {         return p.value.volume; })
        .valueAccessor(function (p) {       return p.value.counts; })
        .radiusValueAccessor(function (p) { return p.value.volume/1000; })
        .maxBubbleRelativeSize(0.3)
        .x(d3.scale.linear().domain([0, 31000000]))
        .y(d3.scale.linear().domain([0, 10]))
        .r(d3.scale.linear().domain([0, 300000]))
        .renderLabel(false)
        .renderTitle(true).title(function (p){
            return [p.key,
                    "Найдено "+numberFormat(p.value.counts)+" контрактов",
                    " на сумму "+numberFormat(p.value.volume)+" руб"
            ].join("\n");
        })
        .xAxisLabel('Контрактов').yAxisLabel('Сумма контрактов');
}

function supplierBubbleChartInit() {
    var spSumGroup      = spDimension.group().reduce(
        function (p, v) { p.volume += +v.volume; p.counts += 1; return p; },
        function (p, v) { p.volume -= +v.volume; p.counts -= 1; return p; },
        function ()     { return {volume: 0, counts: 0} }
    );

    supplierBubbleChart.width(maxWidth/2).height(250)
        .margins({top: 10, right: 50, bottom: 30, left: 40})
        .dimension(spDimension).group(spSumGroup)
        .data(function(group) { return group.top(100); })
        .colors(colorbrewer.RdYlGn[9].reverse()).colorDomain([0, 30])
        .colorAccessor(function (d) {       return d.value.counts; })
        .keyAccessor(function (p) {         return p.value.volume; })
        .valueAccessor(function (p) {       return p.value.counts; })
        .radiusValueAccessor(function (p) { return p.value.volume/1000; })
        .maxBubbleRelativeSize(0.3)
        .x(d3.scale.linear().domain([0, 31000000]))
        .y(d3.scale.linear().domain([0, 10]))
        .r(d3.scale.linear().domain([0, 300000]))
        .renderLabel(false)
        .renderTitle(true).title(function (p){
            return [p.key,
                    "Найдено "+numberFormat(p.value.counts)+" контрактов",
                    " на сумму "+numberFormat(p.value.volume)+" руб"
            ].join("\n");
        })
        .xAxisLabel('Контрактов').yAxisLabel('Сумма контрактов');
}

// -------------------------------------------------------------------------------------------------------------------

function updateScales() {

//    console.log(ctDimension.size);
//    console.log(ctDimension.top(1));
//    console.log(ctDimension.group().reduceSum(function(v) {return v.volume}).top(10));

//    customerBubbleChart.r(d3.scale.log().domain([0, 2]));
//    customerBubbleChart.r(d3.scale.log().domain([0, d3.max(ctDimension, function (d) { return parseInt(d.x) }) * 1.1]));
//    customerBubbleChart.redraw();
}
// -------------------------------------------------------------------------------------------------------------------


function initStatGraph(startDate, stopDate){
    dateChartInit(startDate, stopDate);
    priceChartInit();

    customerBubbleChartInit();
    supplierBubbleChartInit();

    quarterChartInit();
    dayOfWeekChartInit();
    dataTableInit();

    setTimeout(updateScales, 4000);

    dc.dataCount(".dc-data-count").dimension(ndx).group(ndx.groupAll());
//    dc.dataCount(".suppliers-count").dimension(spDimension).group(spDimension.groupAll());
//    dc.dataCount(".customers-count").dimension(ctDimension).group(ctDimension.groupAll());

    dc.renderAll();
}
