var sock = new WebSocket('ws://' + location.host + '/socket');
//var donut;

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

var createBidChampState = function() {
  var obj = {
    items: ko.observable()
  };

  obj.updateUserId = function(userId) {
    obj.userId = userId;
  };

  obj.updateItems = function(items) {
      //donut.setData([
      //  {label: "Odds", value: chance},
      //  {label: "-", value: 100 - chance}
      //]);
      //donut.select(0);

    var nextItems = items.map(function(item) {
      return {
        description: item.item.name + " (£" + item.item.price + ")",
        achieved: (item.percentageAchieved * 100).toFixed(2) + "%",
        imagePath: item.item.imagePath,
        endTime: item.gameEnds,
        odds: item.chanceOfWinning
            ? (item.chanceOfWinning * 100).toFixed(2) + "%"
            : "-",
        timeRemaining: ko.observable(msToTime(new Date(item.gameEnds) - new Date())),
        bid: item.moneySpent
            ? "£" + item.moneySpent
            : "-",
        addBid: function() {
          console.log("Send bid!");
          sock.send(JSON.stringify({
            command: "addToBid",
            payload: {
              item: item.item.name,
              amount: 10
            }
          }));
        }
      };
    });

    obj.items(nextItems);
  };

  setInterval(function() {
    var items = obj.items();
    if (items) {
      items.forEach(function(item) {
        if (item.endTime) {
          var timerRemaining = new Date(item.endTime) - new Date();
          item.timeRemaining(msToTime(timerRemaining));
        }
      });
    }
  }, 500);

  return obj;
};

$(document).ready(function() {
  console.log('Document loaded!');

  var bidChampState = createBidChampState();

  ko.applyBindings(bidChampState, document.getElementById('app-root'));

  sock.onopen = function() {
    console.log('websocket opened');

      // donut = Morris.Donut({
      //   element: 'odds',
      //   data: [
      //     {label: "Odds", value: 0},
      //     {label: "-", value: 100}
      //   ],
      //   formatter: function (y, data) {
      //     return y + '%'
      //   },
      //   colors: [
      //     "#00BBD6",
      //     "#b3d4fc"
      //   ]
      // });
      //donut.select(0);

  };

  sock.onmessage = function(e) {
    console.log('websocket received', e.data);
    var json = JSON.parse(e.data);

    if (json.user) {
      bidChampState.updateUserId(json.user);
    }

    if (json.items){
      bidChampState.updateItems(json.items);
    } else if (json.body) {
      $.notify(json.body, "success", {autoHideDelay: 10000});
    }
  };

  sock.onclose = function() {
    console.log('websocket closed');
  };

});