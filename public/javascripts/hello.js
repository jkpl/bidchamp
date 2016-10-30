var sock = new WebSocket('ws://' + location.host + '/socket');

var redrawTimeLeft = function(element, target) {
  return setInterval(function() {
    var timeRemaining = target - new Date();
    element.text(msToTime(timeRemaining));
  }, 500);
};

function msToTime(duration) {
  if (!Number.isInteger(duration) || duration < 1000) return "00:00:00";

  var seconds = parseInt((duration/1000)%60)
      , minutes = parseInt((duration/(1000*60))%60)
      , hours = parseInt((duration/(1000*60*60))%24);

  hours = (hours < 10) ? "0" + hours : hours;
  minutes = (minutes < 10) ? "0" + minutes : minutes;
  seconds = (seconds < 10) ? "0" + seconds : seconds;

  return hours + ":" + minutes + ":" + seconds ;
}

var bidChampState = (function() {
  var obj = {
    items: []
  };

  function updateCard(item) {
    var d = new Date();
    $("#item-description").text(
        item.item.name + " (£" + item.item.price + ")"
    );
    $("#item-achieved").text(
        (item.percentageAchieved * 100).toFixed(2) + "%"
    );
    $("#item-image").text(item.image);
    $("#item-odds").text(
        item.chanceOfWinning
            ? (item.chanceOfWinning * 100).toFixed(2) + "%"
            : "-"
    );
    if (item.gameEnds) {
      item.timerId = redrawTimeLeft($("#item-time-left"), new Date(item.gameEnds));
    } else {
      $("#item-time-left").text('-')
    }
    $("#item-bid-amount").text(
        item.moneySpent
            ? "£" + item.moneySpent
            : "-"
    );

    $("#item-bid")
        .off('click')
        .on('click', function() {
            console.log("Send bid!");
            sock.send(JSON.stringify({
              command: "addToBid",
              payload: {
                item: "Macbook",
                amount: 10
              }
            }));
        });
  }

  obj.updateUserId = function(userId) {
    obj.userId = userId;
  };

  obj.updateItems = function(items) {
    obj.items.forEach(function(item) {
      if (item.timerId) {
        clearInterval(item.timerId);
      }
    });
    obj.items = items;

    obj.items.forEach(function(item, index) {
      if (item.item.name === 'Macbook') {
        updateCard(item)
      }
    });
  };

  return obj;
}());

sock.onopen = function() {
  console.log('websocket opened');
};

sock.onmessage = function(e) {
  console.log('websocket received', e.data);
  var json = JSON.parse(e.data);

  if (json.user) {
    bidChampState.updateUserId(json.user);
  }

  if (json.items){
    bidChampState.updateItems(json.items);
  } else if (json.eventType === 'NOTIFICATION'){
    $.notify(json.body, "success", {autoHideDelay: 10000, });
  }
};

sock.onclose = function() {
  console.log('websocket closed');
};
