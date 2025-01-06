package com.paylisher.android.notification

import android.util.Log

// TODO: Huawei Mobile Services (HMS)
// https://developer.huawei.com/consumer/en/hms/huawei-pushkit/

//import com.huawei.hms.push.HmsMessageService;
//import com.huawei.hms.push.RemoteMessage;

class HmsMessagingService {
    /*

      //  class HmsMessagingService extends HmsMessageService {
     override fun onNewToken(token: String) {
         Log.d(TAG, "Refreshed token: $token")

         // If you want to send messages to this application instance or
         // manage this apps subscriptions on the server side, send the
         // FCM registration token to your app server.
         sendRegistrationToServer(token)
     }

     private fun sendRegistrationToServer(token: String?) {
         // TODO: Implement this method to send token to your app server.
         Log.d(TAG, "sendRegistrationTokenToServer($token)")
     }

     override fun onMessageReceived(remoteMessage: RemoteMessage) {

     }

    */
    companion object {
        private var TAG = "Hms"
    }
}
