ChatCtrl = ($scope, $http) ->
  $scope.users = []
  $scope.messages = []

  $scope.refresh = () ->
    $http.get("/chat/users").success((data) ->
      $scope.users = data
      $scope.selectedUser = data[0]
    )

  $scope.refresh()

  $scope.closeErrorBox = () -> $scope.errorMessage = null

  $scope.logIn = () ->
    if !$scope.username
      $scope.errorMessage = "You should enter valid username!"
    else
      $http.get("/chat/logIn/" + $scope.username).success((data) ->
        if "success" == data.result
          $scope.users = data.users
          $scope.loggedIn = true
        else
          $scope.errorMessage = data.message
      )

  $scope.send = () ->
    if !$scope.message
      $scope.errorMessage = "You should enter valid message before sending!"
    else
      message =
        user: $scope.username
        text: $scope.message
      $http.post("/chat/send", message).success((data) ->
        if data.result == "success"
          $scope.messages.push message
          $scope.message = null
        else
          $scope.errorMessage = data.message
      )
