function init_calendar(date_start, date_end) {
    var date_format = 'DD.MM.YYYY';

    var date_ranger = $('#date_range').daterangepicker({
        format:     date_format,
        startDate:  date_start,
        endDate:    date_end,
        minDate:    '01.01.2011',
        maxDate:    '31.12.2013',
        showWeekNumbers: false,
        locale: { applyLabel: 'ОК', cancelLabel: 'Отмена', fromLabel: 'От', toLabel: 'До' }
    }, function(start, end, label) {
        $('input[name="date_from"]').val(start.format(date_format))
        $('input[name="date_to"]').val(end.format(date_format))
    });
    $('input[name="date_from"]').val(date_start);
    $('input[name="date_to"]').val(date_end);
}

function init_pricer(price_start, price_end) {
    var price_slider = $('#price_slider').slider({
        tooltip: 'always'
    });
    price_slider.on('slide', function(e) {
        $('input[name="price_from"]').val(e.value[0]);
        $('input[name="price_to"]').val(e.value[1]);
    });

    $('input[name="price_from"]').val(price_start);
    $('input[name="price_to"]').val(price_end);
}

function updateHighOptions() {
    Highcharts.setOptions({
        lang: {
            months: ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь',  'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь'],
            shortMonths: ['Янв', 'Фев', 'Мар', 'Апр', 'Май', 'Июн',  'Июл', 'Авг', 'Сент', 'Окт', 'Ноя', 'Дек']
        }
    });
}

function init_mini_graph(id, url) {
    updateHighOptions();
    $.getJSON(url, function(data) {
        var arr = [];
        $.each( data, function(k, v) {
            arr.push( [v.time, v.count] );
        });
        $(id).highcharts({
            chart: { type: 'column' },
            title: { text: '' },
            yAxis: { title: { text: '' } },
            xAxis: {
                type: 'datetime',
                tickInterval: 3 * 30 * 24 * 3600 * 1000, // one week
//                tickWidth: 0,
//                gridLineWidth: 1,
//                labels: { enabled: false }
            },
            legend: { enabled: false },
            series: [{
                name: 'Контрактов за месяц',
                data: arr
            }]
        });
    });
}

function init_mini_map() {
    var map = showMap(55.7594, 37.6182, 10)
    var areaSelect = L.areaSelect({width: $("#map").width()/4, height: $("#map").height()/4});
    areaSelect.on("change", function() {
        var bounds = this.getBounds();
        $('input[name="geo_f"]').prop('checked', true);
        $('.geo_label').css('font-weight', 'bold');
        $('input[name="sw_lat"]').val(bounds.getSouthWest().lat.toFixed(4));
        $('input[name="sw_lng"]').val(bounds.getSouthWest().lng.toFixed(4));
        $('input[name="ne_lat"]').val(bounds.getNorthEast().lat.toFixed(4));
        $('input[name="ne_lng"]').val(bounds.getNorthEast().lng.toFixed(4));
    });
    areaSelect.addTo(map);
}
