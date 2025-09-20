import type { LogMeta } from "../shared/types";

const getWorklogTablePaddedItem = (targetClass: Record<string, string>, className: string, logMeta: LogMeta) => {
  return <div className={`${targetClass[className]} ${targetClass[logMeta.className]}`}>{logMeta.label}</div>;
};

export default getWorklogTablePaddedItem;
