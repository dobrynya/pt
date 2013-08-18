ChatCtrl = ($scope) ->
  $scope.messages = []
  $scope.users = []

  $scope.send = () ->
    if !$scope.message || !$scope.recipient
      $scope.errorMessage = "You should enter valid message and select recepient before sending!"
    else
      $scope.websocket.send(JSON.stringify(
        kind: "message"
        recipient: $scope.recipient
        text: $scope.message
      ))
      $scope.messages.push
        sender: $scope.username
        text: $scope.message
      $scope.message = null

  $scope.logIn = () ->
    if !$scope.username
      $scope.errorMessage = "You should enter valid username!"
    else
      $scope.websocket = new WebSocket("ws://localhost:9000/chat/logIn/" + $scope.username)
      $scope.websocket.onopen = () ->
        $scope.websocket.send(JSON.stringify(
          kind: "status"
        ))
        $scope.loggedIn = true

      $scope.onclose = () ->
        $scope.loggedIn = false

      $scope.websocket.onmessage = (message) ->
        data = JSON.parse(message.data)
        switch data.kind
          when "tick-tack"
            console.info(JSON.stringify(data.time))
          when "connected"
            $scope.$apply(() ->
              $scope.users = data.users.filter (u) -> u != $scope.username
              $scope.recipient = $scope.users[0]
              $scope.messages.push
                sender: data.user
                text: "I'm connected!"
            )
          when "message"
            $scope.$apply(() ->
              $scope.messages.push({
                sender: data.sender
                text: data.text
              })
            )
          when "disconnected"
            $scope.$apply(() -> $scope.users = $scope.users.filter (u) -> data.user != u)

