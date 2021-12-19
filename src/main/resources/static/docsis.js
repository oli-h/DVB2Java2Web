var dvbApp = angular.module('dvbApp', []);

dvbApp.controller('DocsisController', function DocsisController($scope, $http, $timeout, $interval) {
    const docsis = this;
    var freq;
    docsis.channels = [];

    function collectDocsisStats() {
        $http.get("docsisStats").then(function(resp) {
            var channel = docsis.channels.find(c => c.frequency == resp.data.frequency);
            if (channel) {
                Object.assign(channel, resp.data);
            } else {
                docsis.channels.push(resp.data)
            }
            tuneNext();
        });
    }

    function tuneNext() {
        if (freq >= 538 && freq < 722) {
            freq += 8;
        } else {
            freq = 538
        }
        const tuneParams = { frequency: freq * 1000000, symbol_rate: 6952000, modulation: "QAM_256" }
        $http.post("tune", tuneParams).then(function(resp) {
            collectDocsisStats();
        });
    }

    tuneNext();

    docsis.totalLoad = function() {
        var sumDocsis=0;
        var sumFiller=0;
        docsis.channels.forEach(c => {
            sumDocsis += c.countDocsisPackets;
            sumFiller += c.countFillerPackets;
        });
        return sumDocsis/(sumDocsis+sumFiller);
    }

});
