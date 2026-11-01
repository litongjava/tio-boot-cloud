package nexus.io.tio.boot.cloud.gateway;

import nexus.io.model.body.RespBodyVo;
import nexus.io.tio.http.common.HttpResponse;

/**
 * Error responses written by the gateway itself.
 */
public class GatewayResponses {

  /**
   * Sets the status and a {@code RespBodyVo} JSON body.
   */
  public static HttpResponse error(HttpResponse response, int status, String message) {
    response.setStatus(status);
    response.setJson(RespBodyVo.fail(status, message));
    return response;
  }
}
