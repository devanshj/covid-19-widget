import type { NowRequest, NowResponse } from "@now/node"
import { map } from "fp-ts/lib/Array"
import t from "io-ts"
import { mapTuple } from "./utils";

export default (req: NowRequest, res: NowResponse) =>
    t.union(mapTuple(locations, l => t.literal(l)))
    .decode(req.query.location)
        

const locations = [
    "IN",
    "IN-MH",
    "IN-TN",
    "IN-DL",
    "IN-KL",
    "IN-TG",
    "IN-UP",
    "IN-RJ",
    "IN-AP",
    "IN-MP",
    "IN-KA",
    "IN-GJ",
    "IN-JK",
    "IN-HR",
    "IN-PB",
    "IN-WB",
    "IN-BR",
    "IN-AS",
    "IN-UT",
    "IN-OR",
    "IN-CH",
    "IN-LA",
    "IN-AN",
    "IN-CT",
    "IN-GA",
    "IN-HP",
    "IN-PY",
    "IN-JH",
    "IN-MN",
    "IN-MZ",
    "IN-AR",
    "IN-DN",
    "IN-DD",
    "IN-LD",
    "IN-ML",
    "IN-NL",
    "IN-SK",
    "IN-TR"
] as [string, string, ...string[]];