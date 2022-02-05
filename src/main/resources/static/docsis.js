var dvbApp = angular.module('dvbApp', []);

dvbApp.controller('DocsisController', function DocsisController($scope, $http, $timeout, $interval) {
    const docsis = this;
    var freq;
    var minFreq = 538;
    var maxFreq = 722;

    docsis.rangeAll = function() {
        minFreq = 122;
        maxFreq = 866;
        docsis.channels = [];
    }
    docsis.rangeDocsis = function() {
        minFreq = 538;
        maxFreq = 722;
        docsis.channels = [];
    }
    docsis.rangeDocsis();

    function collectDocsisStats(adapter) {
        $http.get("docsisStats?adapter=" + adapter).then(function(resp) {
            var channel = docsis.channels.find(c => c.frequency == resp.data.frequency);
            Object.assign(channel, resp.data);
        }).finally(function() {
            tuneNext(adapter);
        });
    }

    function tuneNext(adapter) {
        if (freq >= minFreq && freq < maxFreq) {
            freq += 8;
        } else {
            freq = minFreq
        }
        const tuneParams = { frequency: freq * 1000000, symbol_rate: 6952000, modulation: "QAM_256" }
        if (freq < 538 || freq > 722) {
            tuneParams.symbol_rate = 6900000;
        }
        if (freq == 426) {
            tuneParams.modulation = "QAM_64";
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

    tuneNext(0);
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
