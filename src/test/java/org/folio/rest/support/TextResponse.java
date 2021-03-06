package org.folio.rest.support;

public class TextResponse extends Response {
    private final String body;

  public TextResponse(int statusCode, String body) {
    super(statusCode);
    this.body = body;
  }

  public String getBody() {
    return body;
  }

  @Override
  public String toString() {
    return String.format("Status Code: %s Body: %s",
      getStatusCode(), getBody());
  }
}
