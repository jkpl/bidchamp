var sock = new WebSocket('ws://' + location.host + '/socket');

sock.onopen = function() {
  console.log('websocket opened');
};

sock.onmessage = function(e) {
  console.log('websocket received', e.data);
  var json = JSON.parse(e.data);
  updateCard(json.items[0].item);
};

sock.onclose = function() {
  console.log('websocket closed');
};

var updateCard = function(userItem) {
  var d = new Date();
  var timeRemaining = userItem.gameEnds - d.getTime();
  $("#item-name").text(userItem.item.name);
  $("#item-description").text(userItem.description);
  $("#item-image").text(userItem.image);
  $("#item-price").text("£" + userItem.item.price);
  $("#item-odds").text((userItem.chanceOfWinning * 100) + "%");
  $("#item-time-left").text(timeRemaining);
  $("#item-bid-amount").text("£" + userItem.moneySpent);
};
