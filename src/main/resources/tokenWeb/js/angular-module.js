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
                //console.log(key + " : " + JSON.stringify(peer_map[key],null,2)); //works.
            }
        console.log("isCustNode=" + peer_map[demoApp.thisNode]["isCustomerNode"]);

        demoApp.peer_map= peer_map; //state stored for ref in html. rupal 30jan19

        demoApp.isCustomerNode = demoApp.peer_map[demoApp.thisNode]['isCustomerNode'];
        demoApp.isPartnerNode = demoApp.peer_map[demoApp.thisNode]['isPartnerNode'];
        demoApp.txnRowColor = "blue";
        //console.log("isCustNode2=" + demoApp.isCustomerNode + " type = "+ typeof demoApp.isCustomerNode);
        //console.log("isPartnerNode2=" + demoApp.isPartnerNode + " type = "+ typeof demoApp.isPartnerNode);
    });

    demoApp.openModalForTokenCreation = () => {
        const modalInstance = $uibModal.open({
            templateUrl: 'demoAppModal.html',
            controller: 'ModalInstanceCtrl',
            controllerAs: 'modalInstance',
            resolve: {
                demoApp: () => demoApp,
                apiBaseURL: () => apiBaseURL,
                peers: () => peers,
                peer_map: () => peer_map
            }
        });

        modalInstance.result.then(() => {}, () => {});
    };

        demoApp.openModalForCashPayment = () => {
            const modalInstance = $uibModal.open({
                templateUrl: 'demoCashAppModal.html',
                controller: 'ModalInstanceCtrl',
                controllerAs: 'modalInstance',
                resolve: {
                    demoApp: () => demoApp,
                    apiBaseURL: () => apiBaseURL,
                    peers: () => peers,
                    peer_map: () => peer_map
                }
            });

            modalInstance.result.then(() => {}, () => {});
        };


    demoApp.openModalForCouponCreation = () => {
        const modalCouponInstance = $uibModal.open({
            templateUrl: 'demoCouponAppModal.html',
            controller: 'ModalCouponInstanceCtrl',
            controllerAs: 'modalCouponInstance',
            resolve: {
                demoApp: () => demoApp,
                apiBaseURL: () => apiBaseURL,
                peers: () => peers,
                peer_map: () => peer_map
            }
        });

        modalCouponInstance.result.then(() => {}, () => {});
    };

    demoApp.openModalForCouponUpdate = () => {
        const modalCouponInstance = $uibModal.open({
            templateUrl: 'demoCouponUpdateAppModal.html',
            controller: 'ModalCouponInstanceCtrl',
            controllerAs: 'modalCouponInstance',
            resolve: {
                demoApp: () => demoApp,
                apiBaseURL: () => apiBaseURL,
                peers: () => peers,
                peer_map: () => peer_map
            }
        });

        modalCouponInstance.result.then(() => {}, () => {});
    };

    demoApp.openModalForTokenTransfer = () => {
        const modalInstance = $uibModal.open({
            templateUrl: 'demoAppModalTransfer.html',
            controller: 'ModalInstanceCtrl',
            controllerAs: 'modalInstance',
            resolve: {
                demoApp: () => demoApp,
                apiBaseURL: () => apiBaseURL,
                peers: () => peers,
                peer_map: () => peer_map
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
                     peers: () => peers,
                     peer_map: () => peer_map
                 }
             });

             modalInstance.result.then(() => {}, () => {});
    }

    demoApp.getCash = () => $http.get(apiBaseURL + "cash")
        .then((response) => demoApp.cash = Object.keys(response.data)
            .map((key) => response.data[key].state.data)
            .reverse());
    demoApp.getCash();

    demoApp.getTokens = () => $http.get(apiBaseURL + "tokens")
        .then((response) => demoApp.tokens = Object.keys(response.data)
            .map((key) => response.data[key].state.data)
            .reverse());
    demoApp.getTokens();

     demoApp.getTxns = () => $http.get(apiBaseURL + "txns")
        .then((response) => {
            //console.log("Txn Response= " + response.data);
            demoApp.txns = response.data;
        });
     demoApp.getTxns();

    demoApp.getCoupons = () => $http.get(apiBaseURL + "coupons")
        .then((response) => {
            console.log("#getCoupons response data=" + response.data);

            demoApp.coupons = Object.keys(response.data)
                        .map((key) => response.data[key].state.data)
                        .reverse()});
    demoApp.getCoupons();

    //demoApp.getColorFor(txn)
    /*demoApp.getTokensIssuedByMe = () => $http.get(apiBaseURL + "tokens-issued-by-me")
        .then((response) => demoApp.loyalty_tokens = Object.keys(response.data)
           .map((key) => response.data[key].state.data)
           .reverse());
    demoApp.getTokensIssuedByMe();*/
});

app.controller('ModalInstanceCtrl', function ($http, $location, $uibModalInstance, $uibModal, demoApp, apiBaseURL, peers, peer_map) {
    const modalInstance = this;

    modalInstance.peer_map = peer_map;
    modalInstance.peers = peers;
    modalInstance.form = {};
    modalInstance.form.formfilename="c:\\users\\rupal\\corda-attach.zip";
    modalInstance.formError = false;

    // Validate and pay Cash
    modalInstance.payCash = () => {
        if (invalidFormInput()) {
            modalInstance.formError = true;
        } else {
            modalInstance.formError = false;

            $uibModalInstance.close();

            const payCashEndpoint = `${apiBaseURL}issue-cash?owner=${modalInstance.form.counterparty}&amount=${modalInstance.form.value}`;

            // Create PO and handle success / fail responses.
            $http.put(payCashEndpoint).then(
                (result) => {
                    modalInstance.displayMessage(result);
                    demoApp.getCash();
                    demoApp.getTokens();
                    demoApp.getCoupons();
                    demoApp.getTxns();
                },
                (result) => {
                    modalInstance.displayMessage(result);
                }
            );
        }
    };

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
                    demoApp.getCash();
                    demoApp.getTokens();
                    demoApp.getCoupons();
                    demoApp.getTxns();
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
                    demoApp.getCash();
                    demoApp.getTokens();
                    demoApp.getCoupons();
                    demoApp.getTxns();
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
             const sendAttachEndpoint = `${apiBaseURL}send-attachment?filename=${modalInstance.form.formfilename}&newowner=${modalInstance.form.counterparty}`;

             // Create PO and handle success / fail responses.
             $http.put(sendAttachEndpoint).then(
                (result) => {
                    modalInstance.displayMessage(result);
                    demoApp.getCash();
                     demoApp.getTokens();
                     demoApp.getCoupons();
                     demoApp.getTxns();
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

app.controller('ModalCouponInstanceCtrl', function ($http, $location, $uibModalInstance, $uibModal, demoApp, apiBaseURL, peers, peer_map) {
    const modalCouponInstance = this;
    console.log("In ModalCouponInstanceCtrl");
    modalCouponInstance.peer_map = peer_map;
    modalCouponInstance.peers = peers;
    modalCouponInstance.form = {};
    modalCouponInstance.formError = false;

    modalCouponInstance.couponStatuses = ["Coupon_Issued", "Coupon_Sent_to_Customer", "Coupon_Redeemed", "Coupon_Sent_For_Reimbursement", "Coupon_Settled"];


    // Validate and create Coupons.
    modalCouponInstance.create = () => {
        console.log("#modalCouponInstance.create");
        if (invalidFormInput()) {
            modalCouponInstance.formError = true;
        } else {
            modalCouponInstance.formError = false;

            $uibModalInstance.close();

            //For now, the coupon issuer (say Evian) issued the coupon and sends to the distributor.  So that initially, owner=distributor (say SBB). Owner can change later.
            const createCouponEndpoint = `${apiBaseURL}issue-coupon?text=${modalCouponInstance.form.value}&owner=${modalCouponInstance.form.counterparty}&distributor=${modalCouponInstance.form.counterparty}`;
            console.log("#modalCouponInstance.create: endpoint is: " + createCouponEndpoint);
            // Create PO and handle success / fail responses.
            $http.put(createCouponEndpoint).then(
                (result) => {
                    modalCouponInstance.displayMessage(result);
                    demoApp.getTokens();
                    demoApp.getCoupons();
                    demoApp.getTxns();
                    demoApp.getCash();
                },
                (result) => {
                    modalCouponInstance.displayMessage(result);
                }
            );
        }
    };

    // Update Coupons.
    modalCouponInstance.update = () => {
        console.log("#modalCouponInstance.update");
        if (invalidFormInput()) {
            modalCouponInstance.formError = true;
        } else {
            modalCouponInstance.formError = false;

            $uibModalInstance.close();

            const createCouponUpdtEndpoint = `${apiBaseURL}update-coupon?newowner=${modalCouponInstance.form.counterparty}&newstatus=${modalCouponInstance.form.value}`;
            console.log("#modalCouponInstance.update: endpoint is: " + createCouponUpdtEndpoint);

            $http.put(createCouponUpdtEndpoint).then(
                (result) => {
                    modalCouponInstance.displayMessage(result);
                    demoApp.getTokens();
                    demoApp.getCoupons();
                    demoApp.getTxns();
                    demoApp.getCash();
                },
                (result) => {
                    modalCouponInstance.displayMessage(result);
                }
            );
        }
    };

    modalCouponInstance.displayMessage = (message) => {
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
    modalCouponInstance.cancel = () => $uibModalInstance.dismiss();

    // Validate the Coupom.
    function invalidFormInput() {
        return false; //later. isNaN(modalCouponInstance.form.value) || (modalCouponInstance.form.counterparty === undefined);
    }

    // Validate the attachment
    function invalidAttachmtFormInput() {
        return !isNaN(modalCouponInstance.form.formfilename) || (modalCouponInstance.form.counterparty === undefined);
    }
});