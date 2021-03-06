(function(){
	
	var injectParams = ['$scope', '$stateParams', '$state', '$alert', '$translate', 'DevicesService', 'DriversService'];
	
	var DeviceNewController = function($scope, $stateParams, $state, $alert, $translate, DevicesService, DriversService) {

		$translate('DEVICE_CREATED_SUCCESSFULLY').then(function(text) {
			$scope.deviceOKText = text;
		});
		
		$translate('DEVICE_CREATED_ERROR').then(function(text) {
			$scope.deviceErrorText = text;
		});

        $scope.driverInfo = {};
		
		if ($stateParams.driverId) {
			$scope.driver = DriversService.getDriver($stateParams.driverId);
			DriversService.getInfos($stateParams.driverId).then(function(driverInfo) {
				$scope.driverInfo = driverInfo;

			});
		} else {
			$scope.driver = [];
		}

        if(Object.keys($scope.driverInfo).length == 0) {
            $scope.driverInfo["settingsSyntax"] = "N.A.";
            $scope.driverInfo["deviceAddressSyntax"] = "N.A.";
        }

		$scope.device = {
			driver: $scope.driver.id,
			configs: {
				disabled: false
			}
		};
		
		$scope.saveDevice = function() {
			
			if ($scope.deviceForm.$valid) {
				DevicesService.create($scope.device).then(function(resp){
					$alert({content: $scope.deviceOKText, type: 'success'});
					return $state.go('channelconfigurator.devices.index');
				}, function(error) {
					$alert({content: $scope.deviceErrorText, type: 'warning'});
					return $state.go('channelconfigurator.devices.index');
				});
			} else {
				$scope.deviceForm.submitted = true;
			}
		}
		
	};

	DeviceNewController.$inject = injectParams;

	angular.module('openmuc.devices').controller('DeviceNewController', DeviceNewController);

})();