var dvbApp = angular.module('dvbApp', []);

dvbApp.controller('DocsisController', function DocsisController($scope, $http, $timeout, $interval) {
    const docsis = this;
    var freq;
    docsis.channels = [];

    function collectDocsisStats(adapter) {
        $http.get("docsisStats?adapter=" + adapter).then(function(resp) {
            var channel = docsis.channels.find(c => c.frequency == resp.data.frequency);
            Object.assign(channel, resp.data);
        }).finally(function() {
            tuneNext(adapter);
        });
    }

    function tuneNext(adapter) {
        if (freq >= 538 && freq < 722) {
            freq += 8;
        } else {
            freq = 538
        }
        const tuneParams = { frequency: freq * 1000000, symbol_rate: 6952000, modulation: "QAM_256" }
        if (freq < 538 || freq > 722) {
            tuneParams.symbol_rate = 6900000;
        }
        var channel = docsis.channels.find(c => c.frequency == tuneParams.frequency);
        if (!channel) {
            channel = {
                frequency: tuneParams.frequency,
                countDocsisPackets: 0,
                countFillerPackets: 0,
                countUnknownPackets: 0,
            };
            docsis.channels.push(channel)
            docsis.channels.sort((c1,c2) => c1.frequency - c2.frequency);
        }
        channel.tuneResp = "...";
        $http.post("tune?adapter=" + adapter, tuneParams).then(function(resp) {
            channel.tuneResp = resp.data;
            if (resp.data.includes("LOCKED in")) {
                collectDocsisStats(adapter);
            } else {
                tuneNext(adapter);
            }
        });
    }

    tuneNext(1);
    tuneNext(2);
    tuneNext(3);
    tuneNext(4);

    docsis.totalLoad = function() {
        var sumDocsis=0;
        var sumFiller=0;
        var sumUnknown=0;
        docsis.channels.forEach(c => {
            sumDocsis  += c.countDocsisPackets;
            sumFiller  += c.countFillerPackets;
            sumUnknown += c.countUnknownPackets;
        });
        return (sumDocsis+sumUnknown)/(sumDocsis+sumUnknown+sumFiller);
    }

});
