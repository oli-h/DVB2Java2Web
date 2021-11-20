var dvbApp = angular.module('dvbApp', []);

dvbApp.controller('IndexController', function IndexController($scope, $http, $timeout) {
    const index = this;

    index.tuneParams = {
        frequency  : 426000000,
        symbol_rate:   6900000,
        modulation : "QAM_64" ,
    }
    index.transportStreams = [];
    var serviceDescriptors = {};
    var logicalChannelNumbers = {};

    index.tune = function() {
        index.transportStreams = [];
        serviceDescriptors = {};
        logicalChannelNumbers = {};
        index.tuneResponse = "...";
        $http.post("tune", index.tuneParams).then(function(resp) {
            index.tuneResponse =  resp.data;
        });
    }

    index.tuneTs = function(ts) {
        index.tuneParams.frequency   = ts.frequency  ;
        index.tuneParams.symbol_rate = ts.symbol_rate;
        index.tuneParams.modulation  = ts.modulation ;
        index.tune();
    }

    var ws = {};
    function ensureWebSocketConnected() {
        if (ws.readyState === WebSocket.OPEN) {
            return;
        }
        ws = new WebSocket('ws://' + location.host + '/pushChannel');
        ws.onmessage = (ev) => {
            // console.log(ev);
            var msg = JSON.parse(ev.data);
            $scope.$apply(() => {
                if (msg.type == "ts") {
                    var existing = index.transportStreams.find(el => {
                        return el.network_id          == msg.network_id
                            && el.transport_stream_id == msg.transport_stream_id
                            && el.frequency           == msg.frequency
                            && el.modulation          == msg.modulation
                            && el.symbol_rate         == msg.symbol_rate
                            && el.FEC_inner           == msg.FEC_inner
                            && el.services.length     == msg.services.length
                    });
                    if (!existing) {
                        index.transportStreams.push(msg);
                        index.transportStreams.sort((ts1, ts2)=>ts1.frequency-ts2.frequency);
                        existing = msg;
                        existing.clazz = {};
                    }
                    existing.lcns.forEach(lcn => {
                        logicalChannelNumbers[lcn.service_id] = lcn;
                    });
                    existing.services.forEach(s => {
                        var sd = serviceDescriptors[s.service_id];
                        if (sd) {
                            s.serviceDescriptor = sd;
                        }
                        var lcn = logicalChannelNumbers[s.service_id];
                        if (lcn) {
                            s.logicalChannelNumber = lcn;
                        }
                    });
//                    existing.clazz.highlight = true;
//                    $timeout(()=>existing.clazz.highlight=false, 100);
                }
                if (msg.type == "serviceDescriptors") {
                    msg.serviceDescriptors.forEach(sd => {
                        serviceDescriptors[sd.service_id] = sd;
                    })
                }
            });
        }

    }
    ensureWebSocketConnected();

    document.addEventListener('visibilitychange', () => {
        if (document.visibilityState === 'visible') {
            ensureWebSocketConnected();
        }
    });

});
