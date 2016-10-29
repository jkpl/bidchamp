var sock = new WebSocket('ws://' + location.host + '/socket');

sock.onopen = function() {
  console.log('open');
};

sock.onmessage = function(e) {
  console.log('message', e.data);
};

sock.onclose = function() {
  console.log('close');
};