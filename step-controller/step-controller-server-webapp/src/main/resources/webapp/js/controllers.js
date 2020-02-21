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
var initTestDashboard = false;

var tecAdminControllers = angular.module('tecAdminControllers',['components','dataTable','chart.js','step', 'views','ui.bootstrap','reportTree','reportTable','schedulerControllers', 'viz-dashboard-manager']);

function escapeHtml(str) {
	var div = document.createElement('div');
	div.appendChild(document.createTextNode(str));
	return div.innerHTML;
};

function escapeRegExp(string){
	return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'); // $& means the whole matched string
};

tecAdminControllers.factory('executionServices', function($http,$q,$filter,ScreenTemplates) {
	var factory = {};

	factory.getDefaultExecutionParameters = function () {
		return $q(function(resolve, reject) {
		  ScreenTemplates.getScreenInputsByScreenId('executionParameters').then(function(inputs){
				var result = {};
				_.each(inputs, function(input) {
					if(input.options && input.options.length>0) {
						result[input.id] = input.options[0].value;
					} else {
						result[input.id] = '';
					}
				})
				resolve(result);
			})
		})
	};

	return factory
})

tecAdminControllers.directive('executionCommands', ['$rootScope','$http','$location','stateStorage','$uibModal','$timeout','AuthService','schedulerServices','executionServices',
	function($rootScope, $http, $location,$stateStorage,$uibModal,$timeout,AuthService,schedulerServices,executionServices) {
	return {
		restrict: 'E',
		scope: {
			artefact: '&',
			isolateExecution: '=?',
			description: '=', 
			includedTestcases: '&',
			onExecute: '&',
			execution: '='
		},
		templateUrl: 'partials/executionCommands.html',
		link: function($scope, $element, $attr,  $tabsCtrl) {      
			$scope.model = {};

			$scope.authService = AuthService;
			if($scope.execution) {
			  $scope.executionParameters = _.clone($scope.execution.executionParameters.customParameters);
			} else {
			  $scope.executionParameters = {};
			  executionServices.getDefaultExecutionParameters().then(function(defaultParameters){$scope.executionParameters = defaultParameters});
			}
			$scope.isolateExecution = $scope.isolateExecution?$scope.isolateExecution:($scope.execution?$scope.execution.executionParameters.isolatedExecution:false);

			function buildExecutionParams(simulate) {
				var executionParams = {userID:$rootScope.context.userID};
				executionParams.description = $scope.description;
				executionParams.mode = simulate?'SIMULATION':'RUN';
				executionParams.repositoryObject = $scope.artefact();
				executionParams.exports = [];
				executionParams.isolatedExecution = $scope.isolateExecution;
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
				executionParams.customParameters = $scope.executionParameters;
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
				schedulerServices.schedule(executionParams);
			};

		}
	};
}]);

tecAdminControllers.directive('executionProgress', ['$http','$timeout','$interval','stateStorage','$filter','$location','viewFactory','$window','reportTableFactory','ViewRegistry',function($http,$timeout,$interval,$stateStorage,$filter,$location,viewFactory,$window,reportTableFactory,ViewRegistry) {
	return {
		restrict: 'E',
		scope: {
			eid: '=',
			updateTabTitle: '&titleUpdate',
			closeTab: '&closeTab',
			active: '&active'
		},
		controller: function($scope,$location,$anchorScroll,$compile, $element) {
			var eId = $scope.eid;
			console.log('Execution Controller. ID:' + eId);

			$stateStorage.push($scope, eId,{});

			$scope.tabs = {selectedTab:0};

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

			$scope.customPanels = ViewRegistry.getDashlets("execution");
			_.each($scope.customPanels, function(panel) {
				panels[panel.id] = {label:panel.label, show:true, enabled:true}
			})

			$scope.scrollTo = function(viewId) {
				panels[viewId].show=true;
				$location.hash($scope.getPanelId(viewId));
				$anchorScroll();
			}

			$scope.isShowPanel = function(viewId) {return panels[viewId].show};
			$scope.setShowPanel = function(viewId,show) {panels[viewId].show=show};
			$scope.isPanelEnabled = function(viewId) {return panels[viewId].enabled};
			$scope.toggleShowPanel = function(viewId) {panels[viewId].show=!panels[viewId].show};
			$scope.enablePanel = function(viewId, enabled) {panels[viewId].enabled=enabled};
			$scope.getPanelId = function(viewId) {return eId+viewId};
			$scope.getPanelTitle = function(viewId) {return panels[viewId].label};
			$scope.panels = _.map(_.keys(panels),function(viewId){return {id:viewId,label:panels[viewId].label}});

			$scope.configParamTable = {};
			$scope.configParamTable.columns = [ { "title" : "Key"}, {"title" : "Value"}];

			$scope.testCaseTable = {};
			$scope.testCaseTable.columns = [ { "title" : "ID", "visible" : false },
				{"title" : "Name",
				"createdCell" : function (td, cellData, rowData, row, col) {
					var rowScope = $scope.$new(false, $scope);
					$scope.testCaseTable.trackScope(rowScope);
					rowScope.id = rowData[0];
					var content = $compile('<a href uib-tooltip="Drilldown" ng-click="drillDownTestcase(id)">'+cellData+'</a>')(rowScope);
					$(td).empty();
					$(td).append(content);
					// no need to call the $apply here as we already in an angular "thread" in the case of 
					// in memory tables like the testcases. The following is required for serverside tables
					//rowScope.$apply();
				}},
				{ "title" : "Current Operations", "width":"60%", "searchmode":"select","createdCell" : function (td, cellData, rowData, row, col) {
				  var rowScope = $scope.$new(false, $scope);
          $scope.testCaseTable.trackScope(rowScope);
          rowScope.id = cellData;
          var content = $compile('<current-operations report-node-id="id"/>')(rowScope);
          $(td).empty();
          $(td).append(content);
				}},
				{ "title" : "Status", "width":"80px", "searchmode":"select","render": function ( data, type, row ) {
					return '<div class="text-center reportNodeStatus status-' + data +'">'  +data+ '</div>';
				}} ];
			$scope.drillDownTestcase = function(id) {
				$scope.testCaseTable.deselectAll();
				$scope.testCaseTable.select(id);
				$scope.enablePanel("steps",true);
				$scope.scrollTo("steps");
			}
			$scope.testCaseTable.defaultSelection = function(value) {
				var execution = $scope.execution;
				if(execution) {
					var artefactFilter = execution.executionParameters.artefactFilter;
					if(artefactFilter) {
						if(artefactFilter.class=='step.artefacts.filters.TestCaseFilter') {
							return _.contains(artefactFilter.includedNames,value[1]);
						} else if(artefactFilter.class=='step.artefacts.filters.TestCaseIdFilter') {
							return _.contains(artefactFilter.includedIds,value[0]);
						}
					} else {
						return true;
					}
				} else {
					return true;
				}
			}

			var executionViewServices = {
					showNodeInTree : function(nodeId) {
						$http.get('/rest/controller/reportnode/'+nodeId+'/path').then(function(response) {
							$scope.tabs.selectedTab = 1;
							var path = response.data;
							path.shift();
							$scope.reportTreeHandle.expandPath(path);
						})
					},
					showTestCase : function(nodeId) {
						$http.get('/rest/controller/reportnode/'+nodeId+'/path').then(function(response) {
							var path = response.data;
							_.each(path, function(node) {
								if(node.artefact && node.artefact._class == 'TestCase') {
									$scope.testCaseTable.deselectAll();
									$scope.testCaseTable.select(node.artefact.id);
									$scope.enablePanel("testCases",true);
								}
							}); 
							$scope.tabs.selectedTab = 0;
							$scope.scrollTo('testCases');
						})
					},
					getExecution : function() {
						return $scope.execution;
					}
			}

			$scope.stepsTable = reportTableFactory.get(function() {
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
			},$scope, executionViewServices);

			var operationRenderer = {
					'Keyword Call' : {
						renderer: function (details) {
							var html = ": ";
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
											var html = " " + details + "ms";
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
													'default' : {
								            renderer: function (details) {
								              var html = " " + details;
								              return html},
								              icon: '' },  
			};

			$scope.currentOperationsTable = {};
			var renderOperationsHtml = function (data) {
				var renderer = operationRenderer[data.name];
				if(!renderer) {
					renderer = operationRenderer['default'];
				}
				var html = "<div style='margin-top:5px'><strong>" + data.name + "</strong>";
				html+=renderer.renderer(data.details);
				html+="</div>";
				return html;
			}
			$scope.currentOperationsTable.columns = [ 
				{"title" : "Operation", "render": function ( data, type, row ) {
					return renderOperationsHtml(data);
				}}];

			$scope.getIncludedTestcases = function() {
				var selectionMode = $scope.testCaseTable.getSelectionMode?$scope.testCaseTable.getSelectionMode():'all';
				if(selectionMode=='all') {
					return null;
				} else if (selectionMode=='custom' || selectionMode=='none') {
					var includedTestCases = {"by":$scope.execution.executionParameters.repositoryObject.repositoryID=="local"?"id":"name"};
					var result = [];
					if($scope.testCaseTable.getRows!=null) {
						_.each($scope.testCaseTable.getRows(true),function(value){result.push(value[includedTestCases.by=="id"?0:1])});
					}
					includedTestCases.list = result;
					return includedTestCases;
				} else {
					throw "Unsupported selection mode: "+selectionMode;
				}
			}

			$scope.onTestExecutionStarted = function() {
				$scope.closeTab();
			}

			$scope.reportTreeHandle = {};

			$scope.openRtm = function() {
				$window.open($scope.rtmlink, '_blank');
			}
			
			$scope.openLink = function(link,target) {
				$window.open(link, target);
			}
		
		},
		link: function($scope, $element, $rootscope) {
			var eId = $scope.eid;
			$scope.reloadingTable=false;
			$scope.isRefreshing=false;
			$http.get('rest/rtm/rtmlink/' + eId).then(function(response) {
				$scope.rtmlink = response.data.link;
			})

			var refreshTestCaseTable = function() {        
				$http.get('rest/controller/execution/' + eId + '/reportnodes?limit=500&class=step.artefacts.reports.TestCaseReportNode').then(function(response) {
					var data = response.data;
					var dataSet = [];
					if(data.length>0) {
						if(data.length>1&&!$scope.isPanelEnabled('testCases')) {              
							$scope.setShowPanel('steps', false);
							$scope.setShowPanel('testCases', true);
						}
						$scope.enablePanel('testCases', true);
					}

					for (i = 0; i < data.length; i++) {
						dataSet[i] = [ data[i].artefactID, data[i].name, data[i].id, data[i].status];
					}
					$scope.testCaseTable.data = dataSet;
				});
			}

			var refreshExecution = function() {
				$http.get('rest/controller/execution/' + eId).then(function(response) {
					var data = response.data;
					if($scope.execution==null) {
						if($scope.testCaseTable.resetSelection) {
							$scope.testCaseTable.resetSelection();
						}
					}
					$scope.execution = data;
					var dataSet = [];
					var parameters = data.parameters;
					if(parameters) {
						for (i = 0; i < parameters.length; i++) {
							dataSet[i] = [parameters[i].key, parameters[i].value];
						}
						$scope.configParamTable.data = dataSet;
					}
					// Set actual execution to the tab title
					$scope.updateTabTitle()(eId,data);
				});        
			}

			$scope.throughputchart = {};
			$scope.responseTimeByFunctionChart = {};

			var refresh = function() {

				$http.get('rest/views/statusDistributionForFunctionCalls/' + eId).then(function(response) {
					$scope.progress = response.data;
				});

				$http.get('rest/views/statusDistributionForTestcases/' + eId).then(function(response) {
					$scope.testcasesProgress = response.data;
				});

				$http.get('rest/views/errorDistribution/' + eId).then(function(response) {
					$scope.errorDistribution = response.data;
					$scope.countByErrorMsg = [];
					$scope.countByErrorCode = [];
					_.map($scope.errorDistribution.countByErrorMsg, function(val, key) {
						$scope.countByErrorMsg.push({errorMessage:key, errorCount:val})
					});
					_.map($scope.errorDistribution.countByErrorCode, function(val, key) {
						$scope.countByErrorCode.push({errorCode:key, errorCodeCount:val})
					});
				});
				 
				$scope.errorDistributionToggleStates = ['message', 'code'];
				$scope.selectedErrorDistirbutionToggle = 'message';

				$scope.searchStepByError = function(error) {
					$scope.tabs.selectedTab = 0;
					$scope.stepsTable.columns[2].search(escapeRegExp(error));
				}

				if($scope.stepsTable && $scope.stepsTable.Datatable && !$scope.reloadingTable) {
				  $scope.isRefreshing=true;
					$scope.stepsTable.Datatable.ajax.reload(function() {
					  $timeout(function() {
					    $scope.isRefreshing=false;}, false);
					  },false);
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

			refreshAll();

			var refreshFct = function() {
			  if ($scope.autorefresh.enabled) {
  			  refreshExecution();
  			  if($scope.active()) { 
  			    if ($scope.execution==null || $scope.execution.status!='ENDED') {
  			      refresh();
  			      refreshTestCaseTable();
  			    }
  			    else {
    			    $scope.autorefresh.enabled=false;
    			  }
  			  }
			  }
			}
				
			$scope.initAutoRefresh = function (on, interval, autoIncreaseTo) {
			  $scope.autorefresh = {enabled : on, interval : interval, refreshFct: refreshFct, autoIncreaseTo: autoIncreaseTo};
			}
			$scope.autorefresh = {};
			
			/* Viz Perf Dashboard */
			$scope.displaymode = 'managed';
			$scope.topmargin = $element[0].parentNode.parentNode.getBoundingClientRect().top * 2;

			$scope.dashboardsendpoint=[];

			$scope.$watch('execution.status',function(newStatus, oldStatus) {
				if(newStatus) {
					if(newStatus === 'ENDED'){
						console.log('ENDED')
						$scope.init = false;
	
						$scope.isRealTime = '';
						$scope.dashboardsendpoint=[new PerformanceDashboard($scope.eid, 'keyword', 'Keyword')];
						
						if(oldStatus && $scope.autorefresh.enabled) {
							refreshAll();
						} else if (oldStatus == null) {
						  $scope.initAutoRefresh(false,0,0)
						}
					}else{
						$scope.init = false;
	
						$scope.isRealTime = 'Realtime';
						$scope.dashboardsendpoint=[new RealtimePerformanceDashboard($scope.eid, 'keyword', 'Keyword', false)];
						if (oldStatus == null) {
						  $scope.initAutoRefresh(true,100,5000)
						}
					}
				}
			});
			
			$scope.init = false;
			
			// explicit first request trigger (could be done more elegantly by passing a "fireRequestUponStart" argument to viz)
			$scope.$on('dashletinput-initialized', function () {
				console.log('<- dashletinput-initialized')
				if(!$scope.init){
					$scope.init = true;
					
					$(document).ready(function(){
						console.log('-> fireQueryDependingOnContext')
						$scope.$broadcast('fireQueryDependingOnContext');
					});
				}
			});

			$scope.vizRelated = {lockdisplay: false};
			$scope.unwatchlock = $scope.$watch('vizRelated.lockdisplay',function(newvalue) {
				if($scope.displaymode === 'readonly'){
					$scope.displaymode='managed';
				}else{
					if($scope.displaymode === 'managed'){
						$scope.displaymode='readonly';
					}
				}
			});

			$scope.$watch('tabs.selectedTab', function(newvalue, oldvalue){
				//initializing dashboard only when hitting the performance tab
				if(newvalue === 2){
					if($scope.execution.status!=='ENDED') {
					  //must handle this here until we have a dedicated controller per tab
					  $scope.$broadcast('globalsettings-refreshInterval', { 'new': $scope.autorefresh.interval });
            $scope.$broadcast('globalsettings-globalRefreshToggle', { 'new': $scope.autorefresh.enabled });						
					}
					
					$(document).ready(function () {
						$scope.topmargin = $element[0].parentNode.parentNode.getBoundingClientRect().top * 2;
						$scope.$broadcast('resize-widget');
					});
				}else{
					//turning off refresh when clicking other views
					$scope.$broadcast('globalsettings-refreshToggle', { 'new': false });
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

	$scope.updateTabTitle = function(eid, execution) {
		var tab = _.findWhere($scope.tabs, {id:eid});
		tab.title = execution.description;
		tab.execution = execution;
	}

	$scope.getTabExecution = function(eid) {
		return _.findWhere($scope.tabs, {id:eid}).execution;
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

tecAdminControllers.controller('ExecutionListCtrl', ['$scope','$compile','$http','stateStorage','$interval',
	function($scope, $compile, $http,$stateStorage, $interval) {
	$stateStorage.push($scope, 'list',{});

	$scope.table = {};

	var descriptionTemplate = $compile('<report-node-icon node="rootReportNode" /> <a href="#/root/executions/{{executionId}}">{{executionDescription}}</a>')
	var resultTemplate = $compile("<table class=\"executionTableResult\"><tr><td><execution-result execution='execution' /></td><td><status-distribution progress='distribution' /></td></tr></table>");
	var timeTemplate = $compile("<date time='time' />");

	$scope.tabledef = {uid:'executions'}
	$scope.tabledef.columns = function(columns) {
		_.each(_.where(columns,{'title':'ID'}),function(col){col.visible=false});
		_.each(_.where(columns,{'title':'Execution'}),function(col){col.visible=false});
		_.each(_.where(columns,{'title':'Summary'}),function(col){col.visible=false});
		_.each(_.where(columns,{'title':'RootReportNode'}),function(col){col.visible=false});
		_.each(_.where(columns,{'title':'Description'}),function(col){
			col.createdCell =  function (td, cellData, rowData, row, col) {
				var rowScope = $scope.$new(true, $scope);
				$scope.table.trackScope(rowScope);
				rowScope.rootReportNode = JSON.parse(rowData[_.findIndex(columns, function(entry){return entry.title=='RootReportNode'})])
				rowScope.executionId = rowData[0];
				rowScope.executionDescription = cellData;
				var content = descriptionTemplate(rowScope,function(){});
				angular.element(td).html(content);  
				rowScope.$digest();
			};
		});
		_.each(_.where(columns,{'title':'Start time'}),function(col){
			col.createdCell =  function (td, cellData, rowData, row, col) {
				var rowScope = $scope.$new(true, $scope);
				$scope.table.trackScope(rowScope);
				rowScope.time = cellData;
				var content = timeTemplate(rowScope,function(){});
				angular.element(td).html(content);  
				rowScope.$digest();
			};
		});
		_.each(_.where(columns,{'title':'End time'}),function(col){
			col.createdCell =  function (td, cellData, rowData, row, col) {
				var rowScope = $scope.$new(true, $scope);
				$scope.table.trackScope(rowScope);
				rowScope.time = cellData;
				var content = timeTemplate(rowScope,function(){});
				angular.element(td).html(content);  
				rowScope.$digest();
			};
		});
		_.each(_.where(columns,{'title':'Status'}),function(col){
			col.searchmode="select";
			col.render = function ( data, type, row ) {return '<span class="executionStatus status-' + data +'">'  +data+ '</span>';};
		});
		_.each(_.where(columns,{'title':'Result'}),function(col){
			col.createdCell =  function (td, cellData, rowData, row, col) {
				var rowScope = $scope.$new(true, $scope);
				$scope.table.trackScope(rowScope);
				rowScope.execution = JSON.parse(rowData[_.findIndex(columns, function(entry){return entry.title=='Execution'})])
				rowScope.distribution = JSON.parse(rowData[_.findIndex(columns, function(entry){return entry.title=='Summary'})])
				var content = resultTemplate(rowScope,function(){});
				angular.element(td).html(content);  
				rowScope.$digest();
			};
		});
		return columns;
	};
	var refresh = function() {
		if($scope.table && $scope.table.Datatable) {
			$scope.table.Datatable.ajax.reload(null, false);
		}
	}
	$scope.autorefresh = {enabled : true, interval : 5000, refreshFct: refresh};
	
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

tecAdminControllers.directive('autoRefreshCommands', ['$rootScope','$http','$location','stateStorage','$uibModal','$timeout','$interval',
  function($rootScope, $http, $location,$stateStorage,$uibModal,$timeout,$interval) {
  return {
    restrict: 'E',
    scope: {
      autorefresh: '=',
      stInline: '=?'
    },
    templateUrl: 'partials/autoRefreshPopover.html',
    controller: function($scope) {
  
      $scope.autoRefreshPresets = [
        {label:'OFF', value:0},
        {label:'1 second', value:1000},
        {label:'2 seconds', value:2000},
        {label:'5 seconds', value:5000},
        {label:'10 seconds', value:10000},
        {label:'30 seconds', value:30000}
      ];
      
      var manuallyChanged = false;
      
      $scope.changeRefreshInterval = function (newInterval){
        manuallyChanged = true;
        $scope.autorefresh.interval = newInterval;
        if ($scope.autorefresh.interval > 0) {          
          $scope.autorefresh.enabled=true;
        } else {
          $scope.autorefresh.enabled=false;
        }
      }
      
      var refreshTimer;
      
      $scope.startTimer = function() {
        if (angular.isDefined(refreshTimer)) { return; }
      
        if($scope.autorefresh.enabled && $scope.autorefresh.interval > 0) {
          refreshTimer = $interval(function() {     
            $scope.autorefresh.refreshFct();
            if ($scope.autorefresh.autoIncreaseTo && !manuallyChanged && $scope.autorefresh.interval < $scope.autorefresh.autoIncreaseTo) {
              var newInterval = $scope.autorefresh.interval*2;
              $scope.autorefresh.interval = (newInterval < $scope.autorefresh.autoIncreaseTo) ? newInterval : $scope.autorefresh.autoIncreaseTo;
            }
          }, $scope.autorefresh.interval);
        }
      }
      
      $scope.stopTimer = function () {
        if (angular.isDefined(refreshTimer)) {
          $interval.cancel(refreshTimer);
          refreshTimer = undefined;
        }
      }
      
      $scope.$watch('autorefresh.interval',function(newInterval, oldInterval) {
        if (oldInterval != newInterval || !angular.isDefined(refreshTimer)){
          $scope.stopTimer();  
          $rootScope.$broadcast('globalsettings-refreshInterval', { 'new': $scope.autorefresh.interval });
          $scope.startTimer();
        }
      });
      
      $scope.$watch('autorefresh.enabled',function(newStatus, oldStatus) {
        if (angular.isDefined(newStatus) && newStatus != oldStatus) {
          if (newStatus) {
            //refresh as soon as autorefresh is re-activated
            $scope.autorefresh.refreshFct();
          } else {
            //In case autorefresh is stopped by parent, we must set interval to 0 explicitly
            $scope.autorefresh.interval = 0;
          } 
          $rootScope.$broadcast('globalsettings-globalRefreshToggle', { 'new': newStatus });
        }
      });

      $scope.$on('$destroy', function() {
        if (angular.isDefined(refreshTimer)) {
          $interval.cancel(refreshTimer);
        }
      });


    }
  };
}]);

tecAdminControllers.directive('currentOperations', function($http) {
  return {
    restrict: 'E',
    scope: {
      reportNodeId: '='
    },
    controller: function($scope) {
      $http.get("rest/threadmanager/operations/"+$scope.reportNodeId).then(function(response) {
        $scope.currentOperation = response.data;
        
      });          
    },
      
    templateUrl: 'partials/operations/currentOperations.html'}
})