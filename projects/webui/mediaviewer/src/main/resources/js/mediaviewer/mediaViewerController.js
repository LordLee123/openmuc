(function(){

	var injectParams = ['$scope', 'MediaViewerService'];

	var MediaViewerController = function($scope, MediaViewerService) {

		MediaViewerService.getAllMedia().then(function(response) {
			console.log(response);
			for(var key in response) {
				var url = window.location.origin + response[key].file;
				response[key].url = url;
			}
			console.log(response);
			$scope.media = response;
		});
		
	};

	MediaViewerController.$inject = injectParams;

	angular.module('openmuc.mediaviewer').controller('MediaViewerController', MediaViewerController);

})();