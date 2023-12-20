var dvbApp = angular.module('dvbApp', []);

dvbApp.controller('TeletextController', function IndexController($http, $location) {
    const teletext = this;
    teletext.pid = $location.hash();

    var reader;

    teletext.start = () => {
        fetch("pes/" + teletext.pid).then(resp => {
            teletext.stop();
            reader = resp.body.getReader();
            reader.read().then(processChunk);
        });
    }

    teletext.stop = () => {
        if (reader) reader.cancel();
        reader = null;
    }

    function processChunk(chunk) {
        if (chunk.done) {
            console.log("reader done")
            return;
        }
        console.log(chunk.value.length);
        reader.read().then(processChunk);
    }
});
