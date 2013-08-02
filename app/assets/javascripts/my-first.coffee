MyOwnCtrl = ($scope) ->
  $scope.title = "Unknown title"

  $scope.action = () -> alert("MyOwnCtrl is called!")

ListCtrl = ($scope, $http) ->
  $scope.names = ['Dmitry', 'Alex']
  $scope.action = () ->
    alert("Link is pressed!")
  $scope.chosen = ''
  $scope.chosenAction = () ->
    $scope.chosen = $("#choose").val()
  $http.get("test/good/4").success((data) ->
    $scope.g = data
    $("#good").show()
  ).error(() -> $("#error").show())

  $http.get("test/allgoods").success((data) ->
    $scope.allgoods = data
    $("#allgoods").show()
  )
