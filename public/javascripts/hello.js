var sock = new WebSocket('ws://' + location.host + '/socket');

var timeRemaining = 0;

sock.onopen = function() {
  console.log('websocket opened');
};

sock.onmessage = function(e) {
  console.log('websocket received', e.data);
  var json = JSON.parse(e.data);
  if (typeof json.items != 'undefined'){
    updateCard(json.items[1]);
  }
  if (json.eventType === 'NOTIFICATION'){
    $.notify(json.body, "success");
  }
};

sock.onclose = function() {
  console.log('websocket closed');
};

var updateCard = function(userItem) {
  var d = new Date();
  if (userItem.gameEnds) {
    timeRemaining = userItem.gameEnds - d.getTime();
  }
  $("#item-name").text(userItem.item.name);
  $("#item-description").text(userItem.gameStatus);
  $("#item-image").text(userItem.image);
  $("#item-price").text("£" + userItem.item.price);
  $("#item-odds").text((userItem.chanceOfWinning) ? (userItem.chanceOfWinning * 100).toFixed(2) + "%" : "0%");
  $("#item-bid-amount").text((userItem.moneySpent) ? "£" + userItem.moneySpent : "£0");
};

var redrawTimeLeft = function() {
  setInterval(function() {
    timeRemaining = timeRemaining - 1000;
    $("#item-time-left").text(msToTime((timeRemaining < 1000) ? 0 : timeRemaining));
  }, 1000);
};
redrawTimeLeft();

function msToTime(duration) {
  var seconds = parseInt((duration/1000)%60)
      , minutes = parseInt((duration/(1000*60))%60)
      , hours = parseInt((duration/(1000*60*60))%24);

  hours = (hours < 10) ? "0" + hours : hours;
  minutes = (minutes < 10) ? "0" + minutes : minutes;
  seconds = (seconds < 10) ? "0" + seconds : seconds;

  return hours + ":" + minutes + ":" + seconds ;
}

