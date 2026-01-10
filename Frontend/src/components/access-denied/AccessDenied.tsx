import { EmptyState } from "../";

interface AccessDeniedProps {
  message: string;
  fitParent?: boolean;
}

const AccessDenied = (props: AccessDeniedProps) => {
  const { message, fitParent } = props;

  return (
    <EmptyState
      imgSrc="/Assets/access_denied.svg"
      alt="Access Denied"
      title="Access Denied"
      message={message}
      fitParent={fitParent}
    />
  );
};

export default AccessDenied;
