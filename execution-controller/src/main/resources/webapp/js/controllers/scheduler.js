/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
var schedulerController = angular.module('schedulerControllers',['dataTable']);

schedulerController.run(function(ViewRegistry) {
  ViewRegistry.registerDashlet('admin/controller','Scheduler','partials/scheduler/schedulerConfiguration.html');
});

schedulerController.controller('SchedulerCtrl', ['$scope', '$http','stateStorage', '$location', '$uibModal', 'AuthService','Dialogs', 
  function($scope, $http,$stateStorage, $location, $uibModal,AuthService, Dialogs) {
    $stateStorage.push($scope, 'scheduler', {});
    
    $scope.authService = AuthService;
    
    $scope.loadTable = function loadTable() {
      $http.get("rest/controller/task").then(function(response) {
        $scope.schedulerTasks = response.data;
      });
    };
    
	$scope.enableSelected = function(remove) {
      var rows = $scope.datatable.getSelection().selectedItems;
      
      for(i=0;i<rows.length;i++) {
    	  $scope.enableTask(rows[i][0]);       
      }
    };
  
    $scope.toggleTaskState = function(id, currentState) {
      if(currentState) {
        $scope.deleteTask(id, false);
      } else {
        $scope.enableTask(id);
      }
    }
    
    $scope.enableTask = function(id) {
      $http.put("rest/controller/task/"+id).then(function() {
        $scope.loadTable();
      });
    }
  
    $scope.saveTask = function(task) {
      $http.post("rest/controller/task",task).then()
    }
    
	$scope.deleteSelected = function(remove) {
	  var rows = $scope.datatable.getSelection().selectedItems;
	  var itemCount = rows.length
	  if(itemCount == 0) {
	    Dialogs.showErrorMsg("You haven't selected any item")
	  } else {
	    var msg
	    if(itemCount == 1) {
	      msg = remove?'Are you sure you want to delete this item?':'Are you sure you want to disable this item?'
	    } else {
	      msg = remove?'Are you sure you want to delete these '+rows.length+' items?':'Are you sure you want to disable these '+rows.length+' items?'
	    }
      Dialogs.showWarning(msg).then(function() {
        for(i=0;i<rows.length;i++) {
          $scope.deleteTask(rows[i][0], remove);
        }
      })	    
	  }

	};
  
  $scope.executeTask = function(task) {
    $http.post("rest/controller/execution",task.executionsParameters).then(
        function(response) {
          var eId = response.data;
          
          $location.$$search = {};
          $location.path('/root/executions/'+eId);
        });
    };
	
	$scope.editTask = function(id) {
	  $http.get("rest/controller/task/"+id).then(function(response){
	    var task = response.data;
      var modalInstance = $uibModal.open({
        templateUrl: 'partials/scheduler/editTaskDialog.html',
        controller: 'editSchedulerTaskModalCtrl',
        resolve: {
          task: function () {
          return task;
          }
        }
        });
        
      modalInstance.result.then(function (functionParams) {
        $scope.loadTable()}, 
      function (task) {});
    }); 
	}
	
	$scope.askAndDeleteTask = function(id, remove) {
	  var msg = remove?'Are you sure you want to delete this item?':'Are you sure you want to disable this item?'
	  Dialogs.showWarning(msg).then(function() {
	    $scope.deleteTask(id, remove) 
	  })
  }
	
  $scope.deleteTask = function(id, remove) {
    $http.delete("rest/controller/task/"+id+"?remove="+remove).then(function() {
      $scope.loadTable();
    });   
  }
  
	$scope.loadTable($scope,$http);
  }]);

schedulerController.controller('editSchedulerTaskModalCtrl', function ($scope, $uibModalInstance, $http, $location, task) {
	  
  $scope.task = task;

  $scope.executionsParameters = function(value) {
    if(arguments.length) {
      $scope.task.executionsParameters = JSON.parse(value);
      return value;
    } else {
      return JSON.stringify($scope.task.executionsParameters);
    }
  }
  
  $scope.save = function () {  
    $http.post("rest/controller/task",$scope.task).then(
      function(response) {
        $uibModalInstance.close(response.data);
      },function(error) {
        $scope.error = "Invalid CRON expression or server error.";
      });
  };

  $scope.cancel = function () {
    $uibModalInstance.dismiss('cancel');
  };
});

schedulerController.factory('schedulerServices', function($http, $location, $uibModal) {
  var factory = {};

  factory.schedule = function(executionParams) {
    var modalInstance = $uibModal.open({
      templateUrl: 'partials/scheduler/newTaskDialog.html',
      controller: 'newTaskModalCtrl',
      resolve: {
        executionParams: function () {
          return executionParams;
        }
      }
    });
    
    modalInstance.result.then(function (taskParams) {
      $http.post("rest/controller/task",taskParams).then(
          function() {
            $location.path('/root/scheduler/');
          });
      
    }, function () {});
  }
  
  return factory
})

schedulerController.controller('newTaskModalCtrl', function ($scope, $uibModalInstance, executionParams) {
    
  $scope.name = executionParams.description;
  
  $scope.ok = function () {
    var taskParams = {'name': $scope.name, 'cronExpression':$scope.cron, 'executionsParameters':executionParams};
    $uibModalInstance.close(taskParams);
  };

  $scope.applyPreset = function(preset) {
    $scope.cron = preset;
  }
  
  $scope.cancel = function () {
    $uibModalInstance.dismiss('cancel');
  };
});
    
schedulerController.controller('SchedulerConfigurationCtrl', function ($scope, $http) {
  
  $http.get("rest/settings/scheduler_execution_username").then(function(response){
    $scope.executionUser = response.data?response.data:"";
  })
  
  $scope.save = function () {
    $http.post("rest/settings/scheduler_execution_username", $scope.executionUser)
  };
});
