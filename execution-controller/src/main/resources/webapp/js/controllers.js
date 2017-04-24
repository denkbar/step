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
var tecAdminControllers = angular.module('tecAdminControllers',['dataTable','chart.js','step', 'views','ui.bootstrap','reportTree']);

function escapeHtml(str) {
  var div = document.createElement('div');
  div.appendChild(document.createTextNode(str));
  return div.innerHTML;
};

tecAdminControllers.directive('executionCommands', ['$rootScope','$http','$location','stateStorage','$uibModal','$timeout','AuthService',function($rootScope, $http, $location,$stateStorage,$uibModal,$timeout,AuthService) {
  return {
    restrict: 'E',
    scope: {
      artefact: '&',
      description: '=', 
      includedTestcases: '&',
      onExecute: '&',
      execution: '='
    },
    templateUrl: 'partials/executionCommands.html',
    link: function($scope, $element, $attr,  $tabsCtrl) {      
      console.log('Controller executionCommands scope:' + $scope.$id);
      //$stateStorage.push($scope, 'execCmd',{});

      $scope.model = {};
      
      $scope.authService = AuthService;
      
      $scope.$watchCollection('model',function(){
        retrieveInputs();
      })
      
      function retrieveInputs() {        
        params =  _.clone($scope.model);
        params.user = $rootScope.context.userID;
        $http({url:"rest/screens/executionParameters", method:"GET", params:params}).then(function(response){
          var data = response.data;
            $scope.inputs=data;
            
            var oldModel = $scope.model;
            var newModel = {};
            var customParameters = ($scope.execution&&$scope.execution.executionParameters)?$scope.execution.executionParameters.customParameters:null;
            _.each($scope.inputs, function(input) {
              if(oldModel[input.id] != null) {
                newModel[input.id] = oldModel[input.id];                
              } else {
                if(input.options && input.options.length>0) {
                  newModel[input.id] = (customParameters&&customParameters[input.id])?customParameters[input.id]:input.options[0].value;
                } else {
                  newModel[input.id] = '';
                }
              }
            });
            
            $scope.model = newModel;
        });
        
      }
            
      function buildExecutionParams(simulate) {
        var executionParams = {userID:$rootScope.context.userID, profileID:$scope.profileID};
        executionParams.description = $scope.description;
        executionParams.mode = simulate?'SIMULATION':'RUN';
        executionParams.artefact = $scope.artefact();
        executionParams.exports = [];
        var includedTestcases = $scope.includedTestcases();
        if(includedTestcases) {
          if(includedTestcases.by=="id") {
            executionParams.artefactFilter = {"class":"step.artefacts.filters.TestCaseIdFilter","includedIds":includedTestcases.list};            
          } else if(includedTestcases.by=="name") {
            executionParams.artefactFilter = {"class":"step.artefacts.filters.TestCaseFilter","includedNames":includedTestcases.list};            
          } else {
            throw "Unsupported clause "+includedTestcases.by;
          }
        }
        executionParams.customParameters = $scope.model;
        return executionParams;
      }
      
      $scope.execute = function(simulate) {
        var executionParams = buildExecutionParams(simulate);
        
        $http.post("rest/controller/execution",executionParams).then(
          function(response) {
            var eId = response.data;
            
            $location.$$search = {};
            $location.path('/root/executions/'+eId);

            $timeout(function() {
              $scope.onExecute();
            });
            
          });
      };
      $scope.stop = function() {
        $http.get('rest/controller/execution/' + $scope.execution.id + '/stop');
      };
      
      $scope.schedule = function () {

        var executionParams = buildExecutionParams(false);
        
        var modalInstance = $uibModal.open({
          animation: $scope.animationsEnabled,
          templateUrl: 'newTaskModalContent.html',
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

        }, function () {
          $log.info('Modal dismissed at: ' + new Date());
        });
      };
      
    }
  };
}]);

tecAdminControllers.controller('newTaskModalCtrl', function ($scope, $uibModalInstance, executionParams) {
	
  $scope.name = executionParams.description;
  
  $scope.ok = function () {
    var taskParams = {'name': $scope.name, 'cronExpression':$scope.cron, 'executionsParameters':executionParams};
    $uibModalInstance.close(taskParams);
  };

  $scope.cancel = function () {
    $uibModalInstance.dismiss('cancel');
  };
});

tecAdminControllers.directive('executionProgress', ['$http','$timeout','$interval','stateStorage','$filter','$location','viewFactory','$window',function($http,$timeout,$interval,$stateStorage,$filter,$location,viewFactory,$window) {
  return {
    restrict: 'E',
    scope: {
      eid: '=',
      updateTabStatus: '&statusUpdate',
      updateTabTitle: '&titleUpdate',
      closeTab: '&closeTab',
      active: '&active'
    },
    controller: function($scope,$location,$anchorScroll) {
      var eId = $scope.eid;
      console.log('Execution Controller. ID:' + eId);
      $stateStorage.push($scope, eId,{});

      var panels = {
          "testCases":{label:"Test cases",show:false, enabled:false},
          "steps":{label:"Keyword calls",show:true, enabled:true},
          "throughput":{label:"Keyword throughput",show:true, enabled:true},
          "performance":{label:"Performance",show:true, enabled:true},
          "reportTree":{label:"Execution tree",show:true, enabled:true},
          "executionDetails":{label:"Execution details",show:true, enabled:true},
          "parameters":{label:"Execution parameters",show:false, enabled:true},
          "currentOperations":{label:"Current operations",show:true, enabled:true}
      }
      
      $scope.scrollTo = function(viewId) {
        panels[viewId].show=true;
        $location.hash($scope.getPanelId(viewId));
        $anchorScroll();
      }
      
      $scope.isShowPanel = function(viewId) {return panels[viewId].show};
      $scope.isPanelEnabled = function(viewId) {return panels[viewId].enabled};
      $scope.toggleShowPanel = function(viewId) {panels[viewId].show=!panels[viewId].show};
      $scope.enablePanel = function(viewId, enabled) {panels[viewId].enabled=enabled};
      $scope.getPanelId = function(viewId) {return eId+viewId};
      $scope.getPanelTitle = function(viewId) {return panels[viewId].label};
      $scope.panels = _.map(_.keys(panels),function(viewId){return {id:viewId,label:panels[viewId].label}});
      
      $scope.autorefresh = true;

      $scope.configParamTable = {};
      $scope.configParamTable.columns = [ { "title" : "Key"}, {"title" : "Value"}];
      
      $scope.testCaseTable = {};
      $scope.testCaseTable.columns = [ { "title" : "ID", "visible" : false },
                                   {"title" : "Name"},
                                   { "title" : "Status", "width":"80px", "searchmode":"select","render": function ( data, type, row ) {
                                     return '<div class="text-center reportNodeStatus status-' + data +'">'  +data+ '</div>';
                                   }} ];
      $scope.testCaseTable.defaultSelection = function(value) {
        return value[2]!='SKIPPED';
      };
      
      var reportNodeRenderer = {
          'step.artefacts.reports.CallFunctionReportNode' : {
            renderer: function (reportNode) {
              var html = "";
              if(reportNode.name)
                html += '<div><small>' + reportNode.name + '</small></div>';
              if(reportNode.input)
                html += '<div>Input: <small><em>' + escapeHtml(reportNode.input) + '</em></small></div>';
              if(reportNode.output)
                html += '<div>Output: <small><em>' + escapeHtml(reportNode.output) + '</em></small></div>';
              if(reportNode.error) {
                html += '<div><label>Error:</label> <small><em>' + escapeHtml(reportNode.error.msg);
                if(reportNode.attachments && reportNode.attachments.length>0) {
                  html += '. Check the attachments for more details.';
                }
                html += '</em></small></div>';
              }
              return html},
            icon: '' },
            'step.artefacts.reports.EchoReportNode' : {
              renderer: function (reportNode) {
                var html = "";
                if(reportNode.name)
                  html += '<div><small>' + reportNode.name + '</small></div>';
                if(reportNode.echo)
                  html += '<div>Echo: <small><em>' + escapeHtml(reportNode.echo) + '</em></small></div>';
                return html},
              icon: '' },            
          'default' : {
            renderer: function (reportNode) {
              var html = "";
              if(reportNode.name)
                html += '<div><small>' + reportNode.name + '</small></div>';
              if(reportNode.error) {
                html += '<div><label>Error:</label> <small><em>' + escapeHtml(reportNode.error.msg);
                if(reportNode.attachments && reportNode.attachments.length>0) {
                  html += '. Check the attachments for more details.';
                }
                html += '</em></small></div>';
              }
              return html},
            icon: '' },
          };
      
      $scope.showDetails = function(id) {
        $scope.selectedTab=1;
      }
      
      $scope.stepsTable = {};
      $scope.stepsTable.columns = function(columns) {
        _.each(_.where(columns,{'title':'ID'}),function(col){col.visible=false});
        _.each(_.where(columns,{'title':'Begin'}),function(col){col.width="80px"});
        _.each(_.where(columns,{'title':'Step'}),function(col){
          //col.width="50%";
          col.sClass = 'rowDetailsToggle';
          col.render = function ( data, type, row ) {
            var reportNode = JSON.parse(data);
            var renderer = reportNodeRenderer[reportNode._class];
            if(!renderer) {
              renderer = reportNodeRenderer['default'];
            }
            //return JSON.stringify(data)
            var detailsHtml = renderer.renderer(reportNode);
            detailsHtml += '<button type="button" class="btn btn-default" aria-label="Left Align" ng-click="showDetails(\''+row[0]+'\')">' +
            '<span class="glyphicon glyphicon glyphicon glyphicon-play" aria-hidden="true"></span>' +
            '</button> '
            return detailsHtml;
            };
        });
        _.each(_.where(columns,{'title':'Error'}),function(col){
          col.render = function ( data, type, row ) {return '<div><small>'  + escapeHtml(data).replace(/\./g, '.<wbr>') + '</small></div>'};
        });
        _.each(_.where(columns,{'title':'Status'}),function(col){
         col.searchmode="select";
         col.width="80px";
         col.render = function ( data, type, row ) {return '<div class="text-center small reportNodeStatus status-' + data +'">'  +data+ '</div>'};
        });
        _.each(_.where(columns,{'title':'Attachments'}),function(col){
          col.title="";
          col.width="15px";
          col.searchmode="none";
          col.render = function ( data, type, row ) {
            var dropdownHtml;
            if(data!=null&&data.length>0) {
              var data = JSON.parse(data)
              if(data.length>1) {
                dropdownHtml = '<div class="dropdown">'+
                '<span class="glyphicon glyphicon-paperclip dropdown-toggle" aria-hidden="true" data-toggle="dropdown"></span>'+
                '<ul class="dropdown-menu" role="menu" aria-labelledby="dropdownMenu1">';
                for(i=0;i<data.length;i++) {
                  var attachment = data[i];
                  var description = attachment.name?attachment.name:attachment._id
                  var id = attachment._id?attachment._id.$oid:attachment.$oid
                  dropdownHtml = dropdownHtml + '<li role="presentation"><a role="menuitem" tabindex="-1" href="files?uuid='+id+'">'+description+'</a></li>';
                }
                dropdownHtml = dropdownHtml+ '</ul></div>';
              } else if(data!=null&&data.length==1) {
                var attachment = data[0];
                var id = attachment._id?attachment._id.$oid:attachment.$oid
                dropdownHtml = '<a href="files?uuid='+id+'"><span class="glyphicon glyphicon-paperclip dropdown-toggle" aria-hidden="true"></span></a>';
              }
            } else {
              dropdownHtml = '';
            }
            return dropdownHtml;
          }
         });
        return columns;
      };
      $scope.stepsTable.params = function() {
        var filter = {'eid':eId};
        if($scope.testCaseTable.getSelection) {
          var testCaseSelection = $scope.testCaseTable.getSelection();
          // if not all items are selected
          if(testCaseSelection.notSelectedItems.length>0) {
            var testcases = [];
            _.each(testCaseSelection.selectedItems, function(testCase) {testcases.push(testCase[0])})
            filter.testcases = testcases;
          }
        }
        
        return filter;
      };
      
      var operationRenderer = {
          'Keyword Call' : {
            renderer: function (details) {
              var html = "";
              if(details[0]) {
                html += details[0].name;
              } 
              if(details[1]) {
                html += '<div><small>' + details[1].id + '</small></div>';
              }
              if(details[2]) {
                html += '<div><small>' + details[2].agentUrl + '</small></div>';
              }
              // html += '<div>Input: <small><em>' + addWordBreakingPoints(escapeHtml(reportNode.input)) + '</em></small></div>';
              return html},
            icon: '' },
          'Quota acquisition' : {
              renderer: function (details) {
                var html = "";
                if(details) {
                  html += '<div><small>ID: ' + details.id + '(' + details.permits + ')</small></div>';
                  if(details.description)
                    html += '<div><small>' + details.description + '</small></div>';
                } 
                return html},
              icon: '' },
          'Sleep' : {
                renderer: function (details) {
                  var html = details + "ms";
                  return html},
                icon: '' },  
          'Token selection' : {
            renderer: function (details) {
              var html = "";
              if(details && Object.keys(details).length) {
                html += '<div><small><label>Criteria: </label>';
                _.mapObject(details,function(value, key) {
                  html += key + '=' + value.selectionPattern + ","
                })
                html += '</small></div>'
              }
              return html},
            icon: '' },
          };
      
      $scope.currentOperationsTable = {};
      $scope.currentOperationsTable.columns = [ 
                                                {"title" : "Operation", "render": function ( data, type, row ) {
        var renderer = operationRenderer[data.name];
        if(!renderer) {
          renderer = reportNodeRenderer['default'];
        }
        var html = data.name;
        html+=renderer.renderer(data.details);
        return html;
        }}];
      
      $scope.getIncludedTestcases = function() {
        var includedTestCases = {"by":$scope.execution.executionParameters.artefact.repositoryID=="local"?"id":"name"};
        var result = [];
        if($scope.testCaseTable.getRows!=null) {
          _.each($scope.testCaseTable.getRows(true),function(value){result.push(value[includedTestCases.by=="id"?0:1])});
        }
        includedTestCases.list = result;
        return includedTestCases;
      }
      
      $scope.onTestExecutionStarted = function() {
        $scope.closeTab();
      }
      
      $scope.reportTreeHandle = {};

      $scope.openRtm = function() {
        $window.open($scope.rtmlink, '_blank');
      }
    },
    link: function($scope, $element) {
      var eId = $scope.eid;
      
      $http.get('rest/rtm/rtmlink/' + eId).then(function(response) {
        $scope.rtmlink = response.data.link;
      })
      
      var refreshTestCaseTable = function() {        
        $http.get('rest/controller/execution/' + eId + '/reportnodes?limit=500&class=step.artefacts.reports.TestCaseReportNode').then(function(response) {
          var data = response.data;
          var dataSet = [];
          if(data.length>0) {
            $scope.enablePanel('testCases', true);
          }
          
          for (i = 0; i < data.length; i++) {
            dataSet[i] = [ data[i].artefactID, data[i].name, data[i].status];
          }
          $scope.testCaseTable.data = dataSet;
        });
      }
      
      var refreshExecution = function() {
        $http.get('rest/controller/execution/' + eId).then(function(response) {
          var data = response.data;
          $scope.execution = data;
          var dataSet = [];
          var parameters = data.parameters;
          if(parameters) {
            for (i = 0; i < parameters.length; i++) {
              dataSet[i] = [parameters[i].key, parameters[i].value];
            }
            $scope.configParamTable.data = dataSet;
          }
          $scope.updateTabTitle()(eId,data.description);
        });        
      }
      
      $scope.throughputchart = {};
      $scope.responseTimeByFunctionChart = {};
      $scope.performancechart = {};
      
      var refresh = function() {        
        $http.get('rest/views/statusDistributionForFunctionCalls/' + eId).then(function(response) {
          $scope.progress = response.data;
        });
        
        if($scope.stepsTable && $scope.stepsTable.Datatable) {
          $scope.stepsTable.Datatable.ajax.reload(null, false);
        }
        
        viewFactory.getReportNodeStatisticCharts(eId).then(function(charts){
          $scope.throughputchart = charts.throughputchart;
          $scope.responseTimeByFunctionChart = charts.responseTimeByFunctionChart;
          $scope.performancechart = charts.performancechart;
        })
        
        viewFactory.getTimeBasedChart('ErrorRate',eId,'Errors/s').then(function(chart){$scope.errorratechart=chart})
        
        $http.get("rest/threadmanager/operations?eid=" + eId)
        .then(
            function(response) {
              var data = response.data;
              var dataSet = [];
              for (i = 0; i < data.length; i++) {
                if(data[i]) {
                  dataSet.push([data[i]]);
                }
              }
              $scope.currentOperationsTable.data = dataSet;
            });
        
        if($scope.reportTreeHandle.refresh) {
          $scope.reportTreeHandle.refresh();
        }
      };

      $scope.testCaseTable.onSelectionChange = function() {
        refresh();
      };
      
      function refreshAll() {
        refreshExecution();
        refresh();
        refreshTestCaseTable();
      }

      var interval = 100;
      $scope.scheduleNextRefresh = function() {
        $timeout(function(){
          if($scope.autorefresh) {
            refreshExecution();
            if($scope.active()) {
              refresh();
              refreshTestCaseTable();
            }
          }
          interval = Math.min(interval * 2,5000);
          if(!$scope.$$destroyed&&($scope.execution==null||$scope.execution.status!='ENDED')) {
            $scope.scheduleNextRefresh();            
          }
        }, interval);
      }
      $scope.scheduleNextRefresh();
      
      refreshAll();

      $scope.$watch('autorefresh',function(newSatus, oldStatus) {
        // if the timer has already been canceled and autorefresh has been clicked => refresh
        if(newSatus) {
          refreshAll();
        }
      })
      
      $scope.$watch('execution.status',function(newSatus, oldStatus) {
        $scope.updateTabStatus()(eId,newSatus);
        if(newSatus=='ENDED') {
          if(oldStatus&&$scope.autorefresh) {
            refreshAll();
          }
        }
      });
    },
    templateUrl: 'partials/progress.html'
  };
}]);

tecAdminControllers.controller('ExecutionTabsCtrl', ['$scope','$http','stateStorage',
                                                  function($scope, $http,$stateStorag) {
  
  $stateStorag.push($scope, 'executions',{tabs:[{id:'list',title:'Execution list',type:'list'}]});
  if($scope.$state == null) { $scope.$state = 'list' };
  
  $scope.isTabActive = function(id) {
    return id == $scope.$state;
  }
  
  $scope.tabs = $stateStorag.get($scope).tabs;

  $scope.newTab = function(eid, title) {
    $scope.tabs.push({id:eid,title:title,active:false,type:'progress'});
    $stateStorag.store($scope,{tabs: $scope.tabs});
    $scope.selectTab(eid);
  }
  
  $scope.selectTab = function(eid) {
    $scope.$state = eid;
  }

  $scope.updateTabStatus = function(eid, status) {
    _.findWhere($scope.tabs, {id:eid}).status=status;
  }
  
  $scope.updateTabTitle = function(eid, title) {
    _.findWhere($scope.tabs, {id:eid}).title=title;
  }
  
  $scope.getTabStatus = function(eid) {
    return _.findWhere($scope.tabs, {id:eid}).status;
  }
  
  $scope.closeTab = function(eid) {
    var pos=-1;
    var tabs = $scope.tabs;
    for(i=0;i<tabs.length;i++) {
      if(tabs[i].id==eid) {
        pos = i;
      }
    }

    tabs.splice(pos,1);
    if($scope.$state==eid) {
      $scope.$state=tabs[tabs.length-1].id;
    }
    $stateStorag.store($scope,{tabs: $scope.tabs});
  }

  $scope.$watch('$state',function() {
    if($scope.$state!=null&&_.findWhere($scope.tabs, {id:$scope.$state})==null) {
      $scope.newTab($scope.$state,'');
    }
  });
}]);

tecAdminControllers.controller('ExecutionListCtrl', ['$scope','$http','stateStorage','$interval',
        function($scope, $http,$stateStorage, $interval) {
            $stateStorage.push($scope, 'list',{});
            
            $scope.autorefresh = true;
            
            $scope.table = {};
            
            $scope.tabledef = {}
            $scope.tabledef.columns = function(columns) {
              _.each(_.where(columns,{'title':'ID'}),function(col){col.visible=false});
              _.each(_.where(columns,{'title':'Description'}),function(col){
                col.render = function ( data, type, row ) {return '<a href="#/root/executions/'+row[0]+'">'+data+'</a>'};
              });
              _.each(_.where(columns,{'title':'Status'}),function(col){
               col.searchmode="select";
               col.render = function ( data, type, row ) {return '<span class="executionStatus status-' + data +'">'  +data+ '</span>';};
              });
              _.each(_.where(columns,{'title':'Summary'}),function(col){
                col.width="160px";
                col.render = function ( data, type, row ) {
                  var view = JSON.parse(data);
                  if(view.count) {
                    var distribution = view.distribution;
                    return '<div style="width: 150px" class="progress" tooltip="PASSED: '+distribution.PASSED.count+', FAILED: '+distribution.FAILED.count+', TECHNICAL_ERROR: '+distribution.TECHNICAL_ERROR.count+'">'+
                    '<div class="progress-bar status-PASSED" style="width:'+distribution.PASSED.count/view.count*100+'%;">'+
                    distribution.PASSED.count+'</div>' +
                    '<div class="progress-bar status-FAILED" style="width:'+distribution.FAILED.count/view.count*100+'%;">'+
                    distribution.FAILED.count+'</div>' +
                    '<div class="progress-bar status-TECHNICAL_ERROR" style="width:'+distribution.TECHNICAL_ERROR.count/view.count*100+'%;">'+
                    distribution.TECHNICAL_ERROR.count+'</div>' +
                    '</div>';
                  } else {
                    return '';
                  }
               }});
              return columns;
            };
            var refresh = function() {
              if($scope.table) {
                $scope.table.Datatable.ajax.reload(null, false);
              }
            }
            
            var refreshTimer = $interval(function(){
              if($scope.autorefresh){refresh();}}, 5000);
            
            $scope.$on('$destroy', function() {
              $interval.cancel(refreshTimer);
            });
            
            $scope.openExecutionProgressTabForSelection = function() {
              var rows = $scope.datatable.getRows(true);
              for(i=0;i<rows.length;i++) {
                $scope.newTab(rows[i][0],'');
              }
            }
            
            $scope.removeSelected = function() {
                var rows = $scope.datatable.getRows(true);
                
                for(i=0;i<rows.length;i++) {
                    $http.get("rest/controller/execution/"+rows[i][0]+"/stop").then(function() {});          
                }
            };
        } ]);

tecAdminControllers.controller('ArtefactListCtrl', [ '$scope', '$http', 'stateStorage', '$interval',
    function($scope, $http, $stateStorage, $interval) {
      $stateStorage.push($scope, 'artefacts', {});

      $scope.autorefresh = true;

      $scope.table = {};

      $scope.tabledef = {}
      $scope.tabledef.columns = function(columns) {
        _.each(_.where(columns, { 'title' : 'ID' }), function(col) {
          col.visible = false
        });
        _.each(_.where(columns, { 'title' : 'Name' }), function(col) {
          col.render = function(data, type, row) {
            return '<a href="#/root/executions/' + row[0] + '">' + data + '</a>'
          };
        });
        return columns;
      };
    } ]);