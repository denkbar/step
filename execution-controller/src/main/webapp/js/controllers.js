var tecAdminControllers = angular.module('tecAdminControllers',['dataTable','chart.js','n3-line-chart','ui.bootstrap', 'step']);

function escapeHtml(str) {
  var div = document.createElement('div');
  div.appendChild(document.createTextNode(str));
  return div.innerHTML;
};

tecAdminControllers.directive('executionCommands', ['$rootScope','$http','$location','stateStorage','$modal','$timeout',function($rootScope, $http, $location,$stateStorage,$modal,$timeout) {
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
      
      $scope.$watchCollection('model',function(){
        retrieveInputs();
      })
      
      function retrieveInputs() {        
        params =  _.clone($scope.model);
        params.user = $rootScope.context.userID;
        $http({url:"rest/screens/executionParameters", method:"GET", params:params}).success(function(data){
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
          executionParams.artefactFilter = {"class":"step.artefacts.filters.TestCaseFilter","includedNames":includedTestcases};
        }
        executionParams.customParameters = $scope.model;
        return executionParams;
      }
      
      $scope.execute = function(simulate) {
        var executionParams = buildExecutionParams(simulate);
        
        $http.post("rest/controller/execution",executionParams).success(
          function(data) {
            var eId = data;
            
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

        var modalInstance = $modal.open({
          animation: $scope.animationsEnabled,
          templateUrl: 'newTaskModalContent.html',
          controller: 'newTaskModalCtrl',
          resolve: {
            items: function () {
              return $scope.items;
            }
          }
        });

        modalInstance.result.then(function (cronExpression) {
          
          var executionParams = buildExecutionParams(false);
          var taskParams = {'cronExpression':cronExpression, 'executionsParameters':executionParams};
          $http.post("rest/controller/task",taskParams).success(
            function(data) {
              $location.path('/root/scheduler/');
            });

        }, function () {
          $log.info('Modal dismissed at: ' + new Date());
        });
      };
      
    }
  };
}]);

tecAdminControllers.controller('newTaskModalCtrl', function ($scope, $modalInstance) {

  $scope.ok = function () {
    $modalInstance.close($scope.cron);
  };

  $scope.cancel = function () {
    $modalInstance.dismiss('cancel');
  };
});
    

tecAdminControllers.directive('executionProgress', ['$http','$timeout','$interval','stateStorage','$filter','$location',function($http,$timeout,$interval,$stateStorage,$filter,$location) {
  return {
    restrict: 'E',
    scope: {
      eid: '=',
      updateTabStatus: '&statusUpdate',
      updateTabTitle: '&titleUpdate',
      closeTab: '&closeTab',
      active: '&active'
    },
    controller: function($scope,$location) {
      var eId = $scope.eid;
      console.log('Execution Controller. ID:' + eId);
      $stateStorage.push($scope, eId,{});

      $scope.displayTestCasePanel = false;
      $scope.displayTestStepsPanel = true;
      
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
          'step.artefacts.reports.TestStepReportNode' : {
            renderer: function (reportNode) {
              var html = "";
              if(reportNode.name)
                html += '<div><small>' + reportNode.name + '</small></div>';
              if(reportNode.input)
                html += '<div>Input: <small><em>' + escapeHtml(reportNode.input) + '</em></small></div>';
              if(reportNode.output)
                html += '<div>Output: <small><em>' + escapeHtml(reportNode.output) + '</em></small></div>';
              if(reportNode.error)
                html += '<div><label>Error:</label> <small><em>' + escapeHtml(reportNode.error) + '</em></small></div>';
              return html},
            icon: '' },
          'default' : {
            renderer: function (reportNode) {
              var html = "";
              if(reportNode.name)
                html += '<div><small>' + reportNode.name + '</small></div>';
              if(reportNode.error)
                html += '<div><label>Error:</label> <small><em>' + escapeHtml(reportNode.error) + '</em></small></div>';
              return html},
            icon: '' },
          };
      
      
      $scope.stepsTable = {};
      $scope.stepsTable.columns = function(columns) {
        _.each(_.where(columns,{'title':'ID'}),function(col){col.visible=false});
        _.each(_.where(columns,{'title':'Begin'}),function(col){col.sClass = 'rowDetailsToggle';col.width="80px"});
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
            return renderer.renderer(reportNode);
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
                  dropdownHtml = dropdownHtml + '<li role="presentation"><a role="menuitem" tabindex="-1" href="DownloadFileServlet?uuid='+id+'">'+description+'</a></li>';
                }
                dropdownHtml = dropdownHtml+ '</ul></div>';
              } else if(data!=null&&data.length==1) {
                var attachment = data[0];
                var id = attachment._id?attachment._id.$oid:attachment.$oid
                dropdownHtml = '<a href="DownloadFileServlet?uuid='+id+'"><span class="glyphicon glyphicon-paperclip dropdown-toggle" aria-hidden="true"></span></a>';
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
      
      $scope.stepsTable.detailRowRenderer = function(rowData, callback) {
        $http.get('rest/controller/reportnode/'+rowData[0]+'/path').success(function(data) {
          var currentNode = _.last(data);
          var html = '<ul class="list-unstyled node-details">';
          if(currentNode.reportNode && currentNode.reportNode.adapter) {html+='<li><strong>Adapter</strong> <span>'+currentNode.reportNode.adapter+'</span></li>'}
          if(currentNode.reportNode){html+='<li><strong>Duration (ms)</strong> <span>'+currentNode.reportNode.duration+'</span></li>'}
          html+='<li><strong>Stacktrace</strong><div><table class="stacktrace">';
          _.each(data.slice(2), function(entry){
            var node = entry.reportNode;
            var artefact = entry.artefact; 
            html+='<tr><td>'+(artefact?_.last(artefact._class.split('.')):'')+'</td><td>'+node.name+'</td><td>';
            var artefactInstance = node.artefactInstance?node.artefactInstance:artefact; 
            
            _.mapObject(artefactInstance, function(value,key){
              if(['_class','id','_id','name','childrenIDs','customAttributes','attachments','createSkeleton','input','output','expectedOutput'].indexOf(key)==-1) {
                if(value) {html+=key+'='+value+' '}
              }})
            html+='</td></tr>'
          })
          html+='</table></div></li></ul>'
          callback(html);
        })
      }
      
      var operationRenderer = {
          'Adapter Call' : {
            renderer: function (details) {
              var html = "";
              if(details[1]) {
                html += details[1].name;
              } 
              if(details[0].token) {
                html += '<div><small>' + details[0].token.url + '</small></div>';
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
          'Capacity acquisition' : {
              renderer: function (details) {
                var html = "";
                if(details) {
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
              if(details.interests && Object.keys(details.interests).length) {
                html += '<small><label>Criteria: </label>';
                _.mapObject(details.interests,function(value, key) {
                  html += key + '=' + value.selectionPattern + ","
                })
                html += '</small>'
              }
              if(details.attributes && Object.keys(details.attributes).length) {
                html += '<small><label>Attributes: </label>';
                _.mapObject(details.attributes,function(value, key) {
                  html += key + '=' + value + ","
                })
                html += '</small>'
              }
              return html},
            icon: '' },
          };
      
      $scope.currentOperationsTable = {};
      $scope.currentOperationsTable.columns = [ { "title" : "Operation"}, 
                                                {"title" : "Details", "render": function ( data, type, row ) {
        var renderer = operationRenderer[data.name];
        if(!renderer) {
          renderer = reportNodeRenderer['default'];
        }
        return renderer.renderer(data.details);
        }}];
      
      $scope.getIncludedTestcases = function() {
        var result = [];
        if($scope.testCaseTable.getRows!=null) {
          _.each($scope.testCaseTable.getRows(true),function(value){result.push(value[1])});
        }
        return result;
      }
      
      $scope.onTestExecutionStarted = function() {
        $scope.closeTab();
      }
    },
    link: function($scope, $element) {
      var eId = $scope.eid;
      
      $http.get('rest/controller/execution/' + eId + '/rtmlink').success(function(data) {
        $scope.rtmlink = "../rtm/"+data.link;
      })
      
      var refreshTestCaseTable = function() {        
        $http.get('rest/controller/execution/' + eId + '/reportnodes?limit=500&class=step.artefacts.reports.TestCaseReportNode').success(function(data) {
          var dataSet = [];
          for (i = 0; i < data.length; i++) {
            dataSet[i] = [ data[i].artefactID, data[i].name, data[i].status];
          }
          $scope.testCaseTable.data = dataSet;
        });
      }
      
      var refreshExecution = function() {
        $http.get('rest/controller/execution/' + eId).success(function(data) {
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
      
      var refresh = function() {        
        $http.get('rest/progress/' + eId).success(function(data) {
          $scope.progress = data;
        });
        
        if($scope.stepsTable && $scope.stepsTable.Datatable) {
          $scope.stepsTable.Datatable.ajax.reload(null, false);
        }
        
        Chart.defaults.global.animation = false;
        
        $scope.onClick = function (points, evt) {
          console.log(points, evt);
        };
        
        $http.get("rest/controller/execution/" + eId + "/throughput?resolution=20")
            .success(
                function(data) {
                  $scope.data = [[]];
                  $scope.labels = [];
                  $scope.series = ['throughput'];
                  _.each(data.rows,function(value){$scope.labels.push($filter('date')(value.date, 'HH:mm:ss'));$scope.data[0].push(value.value)});
                  $scope.options = {
                    axes : { x : { type : "date", key : "date", labelFunction : d3.time.format("%Y-%m-%d") }, y : { type : "linear" } },
                    series : [ { y : "value", label : "A time series", color : "#9467bd", axis : "y", type : "area", thickness : "2px",
                      id : "series_0" } ], tooltip : { mode : "scrubber", formatter : function(x, y, series) {
                      return x + ' : ' + y;
                    } }, stacks : [], lineMode : "linear", tension : 0.7, drawLegend : true, drawDots : true, columnsHGap : 5 };
                });
        
        $http.get("rest/threadmanager/operations?eid=" + eId)
        .success(
            function(data) {
              var dataSet = [];
              for (i = 0; i < data.length; i++) {
                if(data[i]) {
                  dataSet.push([ data[i].name, data[i]]);
                }
              }
              $scope.currentOperationsTable.data = dataSet;
            });
      };

      $scope.testCaseTable.onSelectionChange = function() {
        refresh();
      };
      
      function refreshAll() {
        refreshExecution();
        refresh();
        refreshTestCaseTable();
      }

      var refreshTimer = $interval(function(){
        if($scope.autorefresh) {
          refreshExecution();
          if($scope.active()) {
            refresh();
            refreshTestCaseTable();
          } 
        }}, 5000);
      
      refreshAll();
      
      $element.on('$destroy', function() {
        $interval.cancel(refreshTimer);
      });
      
      $scope.$watch('autorefresh',function(newSatus, oldStatus) {
        // if the timer has already been canceled and autorefresh has been clicked => refresh
        if(newSatus&&refreshTimer.$$state.status==2) {
          refreshAll();
        }
      })
      
      $scope.$watch('execution.status',function(newSatus, oldStatus) {
        $scope.updateTabStatus()(eId,newSatus);
        if(newSatus=='ENDED') {
          $interval.cancel(refreshTimer);
          if(oldStatus&&$scope.autorefresh) {
            refreshAll();
          }
          $scope.autorefresh = false;
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
                    $http.get("rest/controller/execution/"+rows[i][0]+"/stop").success(function(data) {});          
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