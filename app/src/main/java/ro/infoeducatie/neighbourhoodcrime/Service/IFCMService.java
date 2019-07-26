package ro.infoeducatie.neighbourhoodcrime.Service;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface IFCMService {
    @Headers({
            "Content-Type:",
            "Authorization:key=AAAAqgF_Q2M:APA91bGhpakbvoYu9vMr_W_HeIFWm7ZlyhTRo1mXailTaSUGCxhuJL5SfVbQ-4x_oSIll-unSZftOdRPtTBXEQYLmjRSYp36bsH4CKN6ZNkz4p7mxRziUqMGbyhwbzUIn4TaOxM3xNXL"
    })
    @POST("fcm/send")
    Call<String> sendMessage(@Body String body);
}
