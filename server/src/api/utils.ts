export const mapTuple = <T extends any[], U>(xs: T, mapper: (x: T[number]) => U):
    { [I in keyof T]: T[I] extends T[number] ? U : T[I] } =>
        xs.map(mapper) as any;