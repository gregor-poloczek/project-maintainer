export function replaceElement<T, E>(
  target: T,
  attribute: keyof T,
  element: E,
  predicate: (element: E) => boolean,
): T {
  if (Object.isFrozen(target)) {
    target = { ...target };
  }

  let array: E[] = target[attribute] as E[];
  if (Object.isFrozen(array)) {
    array = [...array];
    (target[attribute] as any) = array;
  }
  const index = array.findIndex((e) => predicate(e));
  array[index] = element;
  return target;
}
