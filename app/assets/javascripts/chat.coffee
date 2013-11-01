ChatCtrl = ($scope) ->
  $scope.messages = []
  $scope.users = []

  $scope.deleteChat = ->
    $scope.websocket.send(JSON.stringify(
      kind: "delete"
      opponent: $scope.recipient
    ))
    $scope.messages = []

  $scope.changeRecipient = ->
    if $scope.recipient
      $scope.websocket.send(JSON.stringify(
        kind: "history"
        opponent: $scope.recipient
      ))
    else
      $scope.messages = []

  $scope.send = ->
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
        created: new Date()
      $scope.message = null

  $scope.logIn = ->
    if !$scope.username
      $scope.errorMessage = "You should enter valid username!"
    else
      $scope.websocket = new WebSocket("ws://localhost:9000/chat/logIn/" + $scope.username)
      $scope.websocket.onopen = () -> $scope.loggedIn = true

      $scope.onclose = () -> $scope.loggedIn = false

      $scope.websocket.onmessage = (message) ->
        data = JSON.parse(message.data)
        switch data.kind
          when "connected"
            $scope.$apply(() ->
              $scope.users = data.users.filter (u) -> u != $scope.username
              $scope.recipient = $scope.users[0]
              $scope.changeRecipient()
              $scope.messages.push
                sender: data.user
                created: new Date
                text: "I'm connected!"
            )
          when "message"
            $scope.$apply(() ->
              if data.sender == $scope.recipient
                $scope.messages.push({
                  sender: data.sender
                  text: data.text
                })
            )
          when "disconnected"
            $scope.$apply(() -> $scope.users = $scope.users.filter (u) -> data.user != u)
          when "error"
            $scope.$apply(() ->
              $scope.errorMessage = data.errorMessage
              $scope.loggedIn = false
            )
          when "history"
            $scope.$apply(() -> $scope.messages = data.messages)
          when "delete"
            $scope.$apply(() -> $scope.messages = [])