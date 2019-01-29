"use strict";

// --------
// WARNING:
// --------

// THIS CODE IS ONLY MADE AVAILABLE FOR DEMONSTRATION PURPOSES AND IS NOT SECURE!
// DO NOT USE IN PRODUCTION!

// FOR SECURITY REASONS, USING A JAVASCRIPT WEB APP HOSTED VIA THE CORDA NODE IS
// NOT THE RECOMMENDED WAY TO INTERFACE WITH CORDA NODES! HOWEVER, FOR THIS
// PRE-ALPHA RELEASE IT'S A USEFUL WAY TO EXPERIMENT WITH THE PLATFORM AS IT ALLOWS
// YOU TO QUICKLY BUILD A UI FOR DEMONSTRATION PURPOSES.

// GOING FORWARD WE RECOMMEND IMPLEMENTING A STANDALONE WEB SERVER THAT AUTHORISES
// VIA THE NODE'S RPC INTERFACE. IN THE COMING WEEKS WE'LL WRITE A TUTORIAL ON
// HOW BEST TO DO THIS.

const app = angular.module('demoAppModule', ['ui.bootstrap']);

// Fix for unhandled rejections bug.
app.config(['$qProvider', function ($qProvider) {
    $qProvider.errorOnUnhandledRejections(false);
}]);

app.controller('DemoAppController', function($http, $location, $uibModal) {
    const demoApp = this;

    // We identify the node.
    const apiBaseURL = "/api/token/";
    let peers = [];
    let peer_map = [];

    $http.get(apiBaseURL + "me").then((response) => demoApp.thisNode = response.data.me);

    $http.get(apiBaseURL + "peers").then((response) => peers = response.data.peers);

    //@see TokenApi#peer-details : returns the json directly, not mapped to "peers"
    $http.get(apiBaseURL + "peer-details").then((response) => {
        peer_map = response.data;
        //console.log("Peer details response: " + JSON.stringify(peer_map));
        for (var key in peer_map) {
            console.log(key + " : " + JSON.stringify(peer_map[key]));
        }
    });

    demoApp.openModalForTokenCreation = () => {
        const modalInstance = $uibModal.open({
            templateUrl: 'demoAppModal.html',
            controller: 'ModalInstanceCtrl',
            controllerAs: 'modalInstance',
            resolve: {
                demoApp: () => demoApp,
                apiBaseURL: () => apiBaseURL,
                peers: () => peers
            }
        });

        modalInstance.result.then(() => {}, () => {});
    };

    demoApp.openModalForTokenTransfer = () => {
        const modalInstance = $uibModal.open({
            templateUrl: 'demoAppModalTransfer.html',
            controller: 'ModalInstanceCtrl',
            controllerAs: 'modalInstance',
            resolve: {
                demoApp: () => demoApp,
                apiBaseURL: () => apiBaseURL,
                peers: () => peers
            }
        });

        modalInstance.result.then(() => {}, () => {});
    };

    demoApp.openModalForAttachmentSend = () => {
             const modalInstance = $uibModal.open({
                 templateUrl: 'demoAppModalAttachment.html',
                 controller: 'ModalInstanceCtrl',
                 controllerAs: 'modalInstance',
                 resolve: {
                     demoApp: () => demoApp,
                     apiBaseURL: () => apiBaseURL,
                     peers: () => peers
                 }
             });

             modalInstance.result.then(() => {}, () => {});
    }

    demoApp.getTokens = () => $http.get(apiBaseURL + "tokens")
        .then((response) => demoApp.tokens = Object.keys(response.data)
            .map((key) => response.data[key].state.data)
            .reverse());

    demoApp.getTokens();

    demoApp.getTokensIssuedByMe = () => $http.get(apiBaseURL + "tokens-issued-by-me")
        .then((response) => demoApp.loyalty_tokens = Object.keys(response.data)
           .map((key) => response.data[key].state.data)
           .reverse());

    //console.log("L-issued tokens size=" + demoApp.tokens.length);
    demoApp.getTokensIssuedByMe();
});

app.controller('ModalInstanceCtrl', function ($http, $location, $uibModalInstance, $uibModal, demoApp, apiBaseURL, peers) {
    const modalInstance = this;

    modalInstance.peers = peers;
    modalInstance.form = {};
    modalInstance.formError = false;

    // Validate and create Tokens.
    modalInstance.create = () => {
        if (invalidFormInput()) {
            modalInstance.formError = true;
        } else {
            modalInstance.formError = false;

            $uibModalInstance.close();

            const createTokenEndpoint = `${apiBaseURL}issue-tokens?owner=${modalInstance.form.counterparty}&numtokens=${modalInstance.form.value}`;

            // Create PO and handle success / fail responses.
            $http.put(createTokenEndpoint).then(
                (result) => {
                    modalInstance.displayMessage(result);
                    demoApp.getTokens();
                },
                (result) => {
                    modalInstance.displayMessage(result);
                }
            );
        }
    };

    modalInstance.transfer = () => {
        if (invalidFormInput()) {
            modalInstance.formError = true;
        } else {
            modalInstance.formError = false;

            $uibModalInstance.close();
            console.log("In #transfer");
            const txfrTokenEndpoint = `${apiBaseURL}transfer-tokens?newowner=${modalInstance.form.counterparty}&numtokens=${modalInstance.form.value}`;

            // Create PO and handle success / fail responses.
            $http.put(txfrTokenEndpoint).then(
                (result) => {
                    modalInstance.displayMessage(result);
                    demoApp.getTokens();
                },
                (result) => {
                    modalInstance.displayMessage(result);
                }
            );
        }
    };

    modalInstance.sendAttachment = () => {
        if (invalidAttachmtFormInput()) {
            modalInstance.formError = true;
        } else {
            modalInstance.formError = false;

             $uibModalInstance.close();
             console.log("In #sendAttachment");
             const txfrTokenEndpoint = `${apiBaseURL}send-attachment?filename=${modalInstance.form.formfilename}&newowner=${modalInstance.form.counterparty}`;

             // Create PO and handle success / fail responses.
             $http.put(txfrTokenEndpoint).then(
                (result) => {
                    modalInstance.displayMessage(result);
                     demoApp.getTokens();
                 },
                 (result) => {
                    modalInstance.displayMessage(result);
                 }
               );
             }
         };

    modalInstance.displayMessage = (message) => {
        const modalInstanceTwo = $uibModal.open({
            templateUrl: 'messageContent.html',
            controller: 'messageCtrl',
            controllerAs: 'modalInstanceTwo',
            resolve: { message: () => message }
        });

        // No behaviour on close / dismiss.
        modalInstanceTwo.result.then(() => {}, () => {});
    };

    // Close create Token modal dialogue.
    modalInstance.cancel = () => $uibModalInstance.dismiss();

    // Validate the Token.
    function invalidFormInput() {
        return isNaN(modalInstance.form.value) || (modalInstance.form.counterparty === undefined);
    }

    // Validate the attachment
    function invalidAttachmtFormInput() {
        return !isNaN(modalInstance.form.formfilename) || (modalInstance.form.counterparty === undefined);
    }
});

// Controller for success/fail modal dialogue.
app.controller('messageCtrl', function ($uibModalInstance, message) {
    const modalInstanceTwo = this;
    modalInstanceTwo.message = message.data;
});