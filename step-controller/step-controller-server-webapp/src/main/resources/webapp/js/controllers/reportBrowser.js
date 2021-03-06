/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
angular.module('reportBrowserControllers', ['step' ])

.run(function(ViewRegistry, EntityRegistry) {
  ViewRegistry.registerView('reportBrowser','partials/reportBrowser.html');
})

.controller('ReportNodeBrowserCtrl', function($scope, stateStorage, $http, $location) {
  stateStorage.push($scope, 'reportBrowser', {});
  
  $scope.tableHandle = {};
  $scope.query = "";
  
  $scope.toExecution = function(eid) {
    $scope.$apply(function() {
      $location.path('/root/executions/'+eid);
    })
  }
  
  $scope.search = function() {
    $scope.tableHandle.reload(true);
  }
  
  $scope.stepsTableServerSideParameters = function() {
    var filter = {'oql':$scope.query};
    return filter;
  };
})
