import request from "@/utils/request";

export function getAccounts(data: object) {
  return request({
    url: "/api/accounts",
    method: "get",
    data,
  });
}
